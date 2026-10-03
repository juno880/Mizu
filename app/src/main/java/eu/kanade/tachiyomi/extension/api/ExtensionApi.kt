package eu.kanade.tachiyomi.extension.api

import android.content.Context
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.LoadResult
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import exh.source.BlacklistedSources
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import logcat.LogPriority
import mihon.domain.extensionrepo.interactor.GetExtensionRepo
import mihon.domain.extensionrepo.interactor.UpdateExtensionRepo
import mihon.domain.extensionrepo.model.ExtensionRepo
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.injectLazy
import java.time.Instant
import kotlin.time.Duration.Companion.days

internal class ExtensionApi {

    private val networkService: NetworkHelper by injectLazy()
    private val preferenceStore: PreferenceStore by injectLazy()
    private val getExtensionRepo: GetExtensionRepo by injectLazy()
    private val updateExtensionRepo: UpdateExtensionRepo by injectLazy()
    private val extensionManager: ExtensionManager by injectLazy()

    // SY -->
    private val sourcePreferences: SourcePreferences by injectLazy()
    // SY <--

    private val json: Json by injectLazy()

    // Mizu -->
    private val protoBuf: ProtoBuf by injectLazy()
    // Mizu <--

    private val lastExtCheck: Preference<Long> by lazy {
        preferenceStore.getLong(Preference.appStateKey("last_ext_check"), 0)
    }

    suspend fun findExtensions(): List<Extension.Available> {
        return withIOContext {
            val disabledRepos = sourcePreferences.disabledRepos().get()
            getExtensionRepo.getAll()
                .filter { it.baseUrl !in disabledRepos }
                .map { async { getExtensions(it) } }
                .awaitAll()
                .flatten()
        }
    }

    private suspend fun getExtensions(extRepo: ExtensionRepo): List<Extension.Available> {
        val repoBaseUrl = extRepo.baseUrl

        // Mizu -->
        // Keiyoushi (and other migrated repos) moved their real extension catalog
        // behind a "repo.json" pointer to a new store format (JSON object or
        // Protobuf). The old index.min.json now only contains "please update"
        // placeholder entries. Try the new format first, and fall back to
        // the legacy index.min.json parsing for repos that haven't migrated
        // (getExtensionsFromStore returns null on any failure so the legacy
        // path below runs unaffected).
        getExtensionsFromStore(repoBaseUrl)?.let { return it }
        // Mizu <--

        return try {
            val response = networkService.client
                .newCall(GET("$repoBaseUrl/index.min.json"))
                .awaitSuccess()

            with(json) {
                response
                    .parseAs<List<ExtensionJsonObject>>()
                    .toExtensions(repoBaseUrl)
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Failed to get extensions from $repoBaseUrl" }
            emptyList()
        }
    }

    // Mizu -->
    private suspend fun getExtensionsFromStore(repoBaseUrl: String): List<Extension.Available>? {
        return try {
            val repoJson = with(json) {
                networkService.client
                    .newCall(GET("$repoBaseUrl/repo.json"))
                    .awaitSuccess()
                    .parseAs<RepoJsonObject>()
            }
            val indexUrl = (repoJson.indexV2 ?: return null).toRawGithubusercontentIfNeeded()

            val storeBytes = networkService.client
                .newCall(GET(indexUrl))
                .awaitSuccess()
                .body
                .bytes()
            val store = decodeAsJsonOrProto<ExtensionStoreObject>(storeBytes)

            val extensionList = store.extensionList
                ?: store.extensionListUrl?.let { listUrl ->
                    val listBytes = networkService.client
                        .newCall(GET(listUrl))
                        .awaitSuccess()
                        .body
                        .bytes()
                    decodeAsJsonOrProto<ExtensionListObject>(listBytes)
                }
                ?: return null

            extensionList.toAvailableExtensions(repoBaseUrl)
        } catch (e: Throwable) {
            logcat(LogPriority.WARN, e) {
                "Failed to get extensions from new store format at $repoBaseUrl, falling back to legacy index"
            }
            null
        }
    }

    // Mizu -->
    private val githubRawRegex =
        """^https://github\.com/([^/]+)/([^/]+)/raw/([^/]+)/(.*)$""".toRegex()

    /**
     * `https://github.com/{owner}/{repo}/raw/{ref}/{path}` doesn't reliably serve
     * raw file content for all HTTP clients — convert it to the direct CDN form.
     */
    private fun String.toRawGithubusercontentIfNeeded(): String {
        val match = githubRawRegex.matchEntire(this) ?: return this
        val (owner, repo, ref, path) = match.destructured
        return "https://raw.githubusercontent.com/$owner/$repo/$ref/$path"
    }
    // Mizu <--

    private inline fun <reified T> decodeAsJsonOrProto(rawBytes: ByteArray): T {
        val bytes = decompressIfGzipped(rawBytes)
        return if (bytes.isNotEmpty() && bytes[0] == '{'.code.toByte()) {
            json.decodeFromString(bytes.decodeToString())
        } else {
            protoBuf.decodeFromByteArray(bytes)
        }
    }

    private fun decompressIfGzipped(bytes: ByteArray): ByteArray {
        val isGzip = bytes.size >= 2 &&
            bytes[0] == 0x1f.toByte() &&
            bytes[1] == 0x8b.toByte()
        if (!isGzip) return bytes
        return java.util.zip.GZIPInputStream(bytes.inputStream()).use { it.readBytes() }
    }
    // Mizu <--

    suspend fun checkForUpdates(
        context: Context,
        fromAvailableExtensionList: Boolean = false,
    ): List<Extension.Installed>? {
        // Limit checks to once a day at most
        if (!fromAvailableExtensionList &&
            Instant.now().toEpochMilli() < lastExtCheck.get() + 1.days.inWholeMilliseconds
        ) {
            return null
        }

        // Update extension repo details
        updateExtensionRepo.awaitAll()

        val extensions = if (fromAvailableExtensionList) {
            extensionManager.availableExtensionsFlow.value
        } else {
            findExtensions().also { lastExtCheck.set(Instant.now().toEpochMilli()) }
        }

        // SY -->
        val blacklistEnabled = sourcePreferences.enableSourceBlacklist().get()
        // SY <--

        val installedExtensions = ExtensionLoader.loadExtensions(context)
            .filterIsInstance<LoadResult.Success>()
            .map { it.extension }
            // SY -->
            .filterNot { it.isBlacklisted(blacklistEnabled) }
        // SY <--

        val extensionsWithUpdate = mutableListOf<Extension.Installed>()
        for (installedExt in installedExtensions) {
            val pkgName = installedExt.pkgName
            val availableExt = extensions.find { it.pkgName == pkgName } ?: continue
            val hasUpdatedVer = availableExt.versionCode > installedExt.versionCode
            val hasUpdatedLib = availableExt.libVersion > installedExt.libVersion
            val hasUpdate = hasUpdatedVer || hasUpdatedLib
            if (hasUpdate) {
                extensionsWithUpdate.add(installedExt)
            }
        }

        if (extensionsWithUpdate.isNotEmpty()) {
            ExtensionUpdateNotifier(context).promptUpdates(extensionsWithUpdate.map { it.name })
        }

        return extensionsWithUpdate
    }

    private fun List<ExtensionJsonObject>.toExtensions(repoUrl: String): List<Extension.Available> {
        return this
            .filter {
                val libVersion = it.extractLibVersion()
                libVersion >= ExtensionLoader.LIB_VERSION_MIN && libVersion <= ExtensionLoader.LIB_VERSION_MAX
            }
            .map {
                Extension.Available(
                    name = it.name.substringAfter("Tachiyomi: "),
                    pkgName = it.pkg,
                    versionName = it.version,
                    versionCode = it.code,
                    libVersion = it.extractLibVersion(),
                    lang = it.lang,
                    isNsfw = it.nsfw == 1,
                    sources = it.sources?.map(extensionSourceMapper).orEmpty(),
                    apkName = it.apk,
                    iconUrl = "$repoUrl/icon/${it.pkg}.png",
                    repoUrl = repoUrl,
                )
            }
    }

    fun getApkUrl(extension: Extension.Available): String {
        // Mizu: use the known full URL when available (new store format)
        // instead of always reconstructing one, which can point at the wrong
        // location if the real hosting path doesn't match this convention.
        return extension.fullApkUrl ?: "${extension.repoUrl}/apk/${extension.apkName}"
    }

    private fun ExtensionJsonObject.extractLibVersion(): Double {
        return version.substringBeforeLast('.').toDouble()
    }

    // SY -->
    private fun Extension.isBlacklisted(
        blacklistEnabled: Boolean = sourcePreferences.enableSourceBlacklist().get(),
    ): Boolean {
        return pkgName in BlacklistedSources.BLACKLISTED_EXTENSIONS && blacklistEnabled
    }
    // SY <--
}

@Serializable
private data class ExtensionJsonObject(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String,
    val code: Long,
    val version: String,
    val nsfw: Int,
    val sources: List<ExtensionSourceJsonObject>?,
)

@Serializable
private data class ExtensionSourceJsonObject(
    val id: Long,
    val lang: String,
    val name: String,
    val baseUrl: String,
)

private val extensionSourceMapper: (ExtensionSourceJsonObject) -> Extension.Available.Source = {
    Extension.Available.Source(
        id = it.id,
        lang = it.lang,
        name = it.name,
        baseUrl = it.baseUrl,
    )
}

// Mizu -->
// Models for the newer extension store format (repo.json -> index_v2 -> JSON
// object or Protobuf). Field numbers on the Proto* classes must exactly match
// upstream Mihon's mihon.data.extension.model.NetworkExtensionStore schema
// (confirmed against Mihon v0.20.1 source) since they're used to decode raw
// protobuf bytes, not just JSON.

@Serializable
private data class RepoJsonObject(
    @SerialName("index_v2")
    val indexV2: String? = null,
)

@Serializable
private data class ExtensionStoreObject(
    @ProtoNumber(1) val name: String = "",
    @ProtoNumber(2) val badgeLabel: String = "",
    @ProtoNumber(3) val signingKey: String = "",
    @ProtoNumber(4) val contact: ContactObject? = null,
    @ProtoNumber(101) val extensionList: ExtensionListObject? = null,
    @ProtoNumber(102) val extensionListUrl: String? = null,
)

@Serializable
private data class ContactObject(
    @ProtoNumber(1) val website: String = "",
    @ProtoNumber(2) val discord: String? = null,
)

@Serializable
private data class ExtensionListObject(
    @ProtoNumber(1) val extensions: List<ExtensionEntryObject> = emptyList(),
) {
    fun toAvailableExtensions(repoUrl: String): List<Extension.Available> {
        return extensions
            .mapNotNull { ext ->
                val libVersion = ext.extensionLib.toDoubleOrNull() ?: return@mapNotNull null
                if (libVersion < ExtensionLoader.LIB_VERSION_MIN || libVersion > ExtensionLoader.LIB_VERSION_MAX) {
                    return@mapNotNull null
                }
                val langs = ext.sources.map { it.language }.toSet()
                Extension.Available(
                    name = ext.name,
                    pkgName = ext.packageName,
                    versionName = ext.versionName,
                    versionCode = ext.versionCode,
                    libVersion = libVersion,
                    lang = if (langs.size == 1) langs.first() else "all",
                    isNsfw = ext.contentWarning == ContentWarningEnum.MIXED ||
                        ext.contentWarning == ContentWarningEnum.NSFW,
                    sources = ext.sources.map { source ->
                        Extension.Available.Source(
                            id = source.id,
                            lang = source.language,
                            name = source.name,
                            baseUrl = source.homeUrl,
                        )
                    },
                    apkName = ext.resources.apkUrl.substringAfterLast('/'),
                    iconUrl = ext.resources.iconUrl,
                    repoUrl = repoUrl,
                    // Mizu -->
                    fullApkUrl = ext.resources.apkUrl,
                    // Mizu <--
                )
            }
    }
}

@Serializable
private data class ExtensionEntryObject(
    @ProtoNumber(1) val name: String = "",
    @ProtoNumber(2) val packageName: String = "",
    @ProtoNumber(3) val resources: ResourcesObject = ResourcesObject(),
    @ProtoNumber(4) val extensionLib: String = "",
    @ProtoNumber(5) val versionCode: Long = 0,
    @ProtoNumber(6) val versionName: String = "",
    @ProtoNumber(7) val contentWarning: ContentWarningEnum = ContentWarningEnum.UNSPECIFIED,
    @ProtoNumber(8) val sources: List<SourceEntryObject> = emptyList(),
)

@Serializable
private data class ResourcesObject(
    @ProtoNumber(1) val apkUrl: String = "",
    @ProtoNumber(2) val iconUrl: String = "",
)

@Serializable
private data class SourceEntryObject(
    @ProtoNumber(1) val id: Long = 0,
    @ProtoNumber(2) val name: String = "",
    @ProtoNumber(3) val language: String = "",
    @ProtoNumber(4) val homeUrl: String = "",
    @ProtoNumber(5) val mirrorUrls: List<String> = emptyList(),
    @ProtoNumber(7) val message: String? = null,
)

@Serializable
private enum class ContentWarningEnum {
    @ProtoNumber(0) UNSPECIFIED,
    @ProtoNumber(1) SAFE,
    @ProtoNumber(2) MIXED,
    @ProtoNumber(3) NSFW,
}
// Mizu <--
