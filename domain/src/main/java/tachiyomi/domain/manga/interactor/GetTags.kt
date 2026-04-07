package tachiyomi.domain.manga.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.manga.model.Tag
import tachiyomi.domain.manga.repository.TagRepository

class GetTags(
    private val tagRepository: TagRepository,
) {
    suspend fun await(): List<Tag> = tagRepository.getAllTags()

    fun subscribe(): Flow<List<Tag>> = tagRepository.getAllTagsAsFlow()

    suspend fun awaitForManga(mangaId: Long): List<Tag> = tagRepository.getTagsForManga(mangaId)

    fun subscribeForManga(mangaId: Long): Flow<List<Tag>> = tagRepository.getTagsForMangaAsFlow(mangaId)
}
