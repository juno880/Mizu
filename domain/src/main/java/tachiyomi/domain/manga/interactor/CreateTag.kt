package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.repository.TagRepository

class CreateTag(
    private val tagRepository: TagRepository,
) {
    suspend fun await(name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return -1L
        return tagRepository.insertTag(trimmed)
    }
}
