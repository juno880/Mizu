package mihon.domain.extensionrepo.interactor

import logcat.LogPriority
import mihon.domain.extensionrepo.exception.SaveExtensionRepoException
import mihon.domain.extensionrepo.model.ExtensionRepo
import mihon.domain.extensionrepo.repository.ExtensionRepoRepository
import mihon.domain.extensionrepo.service.ExtensionRepoService
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import tachiyomi.core.common.util.system.logcat

class CreateExtensionRepo(
    private val repository: ExtensionRepoRepository,
    private val service: ExtensionRepoService,
) {
    // Mizu -->
    // Known index file suffixes repos are commonly shared with. Whichever one
    // (if any) the pasted URL ends with gets stripped to recover the base repo
    // URL, since fetchRepoDetails() only ever needs the base URL.
    private val knownIndexSuffixes = listOf("/index.min.json", "/index.pb", "/index.json", "/repo.json")

    private val githubRawRegex =
        """^https://github\.com/([^/]+)/([^/]+)/raw/([^/]+)/(.*)$""".toRegex()

    /**
     * `https://github.com/{owner}/{repo}/raw/{ref}/{path}` doesn't reliably serve
     * raw file content for all HTTP clients — convert it to the direct CDN form:
     * `https://raw.githubusercontent.com/{owner}/{repo}/{ref}/{path}`
     */
    private fun String.toRawGithubusercontentIfNeeded(): String {
        val match = githubRawRegex.matchEntire(this) ?: return this
        val (owner, repo, ref, path) = match.destructured
        return "https://raw.githubusercontent.com/$owner/$repo/$ref/$path"
    }
    // Mizu <--

    suspend fun await(indexUrl: String): Result {
        val formattedUrl = indexUrl.toHttpUrlOrNull()?.toString()
            ?: return Result.InvalidUrl

        // Mizu -->
        // Accept a URL ending in any known index file name (stripped to get the
        // base URL) or a bare base repo URL directly — covers the legacy
        // index.min.json form, the newer index.pb form Keiyoushi now shares,
        // and a plain base URL with nothing appended. Also normalizes the
        // github.com/.../raw/... shorthand to raw.githubusercontent.com.
        val normalizedUrl = formattedUrl.toRawGithubusercontentIfNeeded()
        val matchedSuffix = knownIndexSuffixes.firstOrNull { normalizedUrl.endsWith(it) }
        val baseUrl = if (matchedSuffix != null) {
            normalizedUrl.removeSuffix(matchedSuffix)
        } else {
            normalizedUrl.removeSuffix("/")
        }
        // Mizu <--

        return service.fetchRepoDetails(baseUrl)?.let { insert(it) } ?: Result.InvalidUrl
    }

    private suspend fun insert(repo: ExtensionRepo): Result {
        return try {
            repository.insertRepo(
                repo.baseUrl,
                repo.name,
                repo.shortName,
                repo.website,
                repo.signingKeyFingerprint,
            )
            Result.Success
        } catch (e: SaveExtensionRepoException) {
            logcat(LogPriority.WARN, e) { "SQL Conflict attempting to add new repository ${repo.baseUrl}" }
            return handleInsertionError(repo)
        }
    }

    /**
     * Error Handler for insert when there are trying to create new repositories
     *
     * SaveExtensionRepoException doesn't provide constraint info in exceptions.
     * First check if the conflict was on primary key. if so return RepoAlreadyExists
     * Then check if the conflict was on fingerprint. if so Return DuplicateFingerprint
     * If neither are found, there was some other Error, and return Result.Error
     *
     * @param repo Extension Repo holder for passing to DB/Error Dialog
     */
    private suspend fun handleInsertionError(repo: ExtensionRepo): Result {
        val repoExists = repository.getRepo(repo.baseUrl)
        if (repoExists != null) {
            return Result.RepoAlreadyExists
        }
        val matchingFingerprintRepo = repository.getRepoBySigningKeyFingerprint(repo.signingKeyFingerprint)
        if (matchingFingerprintRepo != null) {
            return Result.DuplicateFingerprint(matchingFingerprintRepo, repo)
        }
        return Result.Error
    }

    sealed interface Result {
        data class DuplicateFingerprint(val oldRepo: ExtensionRepo, val newRepo: ExtensionRepo) : Result
        data object InvalidUrl : Result
        data object RepoAlreadyExists : Result
        data object Success : Result
        data object Error : Result
    }
}
