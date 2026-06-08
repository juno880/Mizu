package eu.kanade.domain.manga.interactor

import eu.kanade.domain.manga.model.DISABLE_AUTO_SHIFT_DOUBLE_PAGES_FLAG
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository

class SetMangaViewerFlags(
    private val mangaRepository: MangaRepository,
) {

    suspend fun awaitSetReadingMode(id: Long, flag: Long) {
        val manga = mangaRepository.getMangaById(id)
        mangaRepository.update(
            MangaUpdate(
                id = id,
                viewerFlags = manga.viewerFlags.setFlag(flag, ReadingMode.MASK.toLong()),
            ),
        )
    }

    suspend fun awaitSetOrientation(id: Long, flag: Long) {
        val manga = mangaRepository.getMangaById(id)
        mangaRepository.update(
            MangaUpdate(
                id = id,
                viewerFlags = manga.viewerFlags.setFlag(flag, ReaderOrientation.MASK.toLong()),
            ),
        )
    }

    // Mizu -->
    suspend fun awaitSetAutoShiftDoublePages(id: Long, enabled: Boolean) {
        val manga = mangaRepository.getMangaById(id)
        val newFlags = if (enabled) {
            manga.viewerFlags and DISABLE_AUTO_SHIFT_DOUBLE_PAGES_FLAG.inv()
        } else {
            manga.viewerFlags or DISABLE_AUTO_SHIFT_DOUBLE_PAGES_FLAG
        }
        mangaRepository.update(MangaUpdate(id = id, viewerFlags = newFlags))
    }
    // Mizu <--

    private fun Long.setFlag(flag: Long, mask: Long): Long {
        return this and mask.inv() or (flag and mask)
    }
}
