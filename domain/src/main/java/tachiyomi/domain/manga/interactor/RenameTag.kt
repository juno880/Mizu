package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.repository.TagRepository

class RenameTag(
    private val tagRepository: TagRepository,
) {
    suspend fun await(tagId: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        tagRepository.renameTag(tagId, trimmed)
    }
}
