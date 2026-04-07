package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.repository.TagRepository

class SetTagsForManga(
    private val tagRepository: TagRepository,
) {
    suspend fun await(mangaId: Long, tagIds: List<Long>) =
        tagRepository.setTagsForManga(mangaId, tagIds)
}
