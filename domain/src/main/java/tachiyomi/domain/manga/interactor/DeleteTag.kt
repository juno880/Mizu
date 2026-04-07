package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.repository.TagRepository

class DeleteTag(
    private val tagRepository: TagRepository,
) {
    suspend fun await(tagId: Long) = tagRepository.deleteTag(tagId)
}
