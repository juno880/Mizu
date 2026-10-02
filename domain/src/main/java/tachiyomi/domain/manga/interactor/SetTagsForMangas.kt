package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.repository.TagRepository

class SetTagsForMangas(
    private val tagRepository: TagRepository,
) {
    suspend fun await(mangaTagIds: Map<Long, List<Long>>) =
        tagRepository.setTagsForMangas(mangaTagIds)
}
