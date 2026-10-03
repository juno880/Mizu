package eu.kanade.tachiyomi.data.coil

import android.app.Application
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.Buffer
import okio.FileSystem
import uy.kohesive.injekt.injectLazy
import java.util.zip.ZipInputStream

/**
 * A [Fetcher] that reads the first image entry out of a local .cbz/.zip chapter
 * file as a thumbnail, for sources/chapters without a dedicated remote
 * thumbnail endpoint.
 */
class CbzCoverFetcher(
    private val data: CbzCoverData,
) : Fetcher {

    private val context: Application by injectLazy()

    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif")

    override suspend fun fetch(): FetchResult? {
        val inputStream = context.contentResolver.openInputStream(data.uri) ?: return null

        inputStream.use { stream ->
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val extension = entry.name.substringAfterLast('.', "").lowercase()
                    if (!entry.isDirectory && extension in imageExtensions) {
                        val buffer = Buffer()
                        buffer.write(zip.readBytes())
                        return SourceFetchResult(
                            source = ImageSource(source = buffer, fileSystem = FileSystem.SYSTEM),
                            mimeType = "image/$extension".let { if (extension == "jpg") "image/jpeg" else it },
                            dataSource = DataSource.DISK,
                        )
                    }
                    entry = zip.nextEntry
                }
            }
        }

        return null
    }

    class Factory : Fetcher.Factory<CbzCoverData> {
        override fun create(data: CbzCoverData, options: Options, imageLoader: ImageLoader): Fetcher {
            return CbzCoverFetcher(data)
        }
    }
}
