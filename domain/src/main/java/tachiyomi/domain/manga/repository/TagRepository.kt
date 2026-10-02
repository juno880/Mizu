package tachiyomi.domain.manga.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.manga.model.Tag

interface TagRepository {

    suspend fun getAllTags(): List<Tag>

    fun getAllTagsAsFlow(): Flow<List<Tag>>

    suspend fun getTagsForManga(mangaId: Long): List<Tag>

    fun getTagsForMangaAsFlow(mangaId: Long): Flow<List<Tag>>

    suspend fun getMangaIdsForTag(tagId: Long): List<Long>

    suspend fun insertTag(name: String): Long

    suspend fun deleteTag(tagId: Long)

    suspend fun renameTag(tagId: Long, name: String)

    suspend fun addTagToManga(mangaId: Long, tagId: Long)

    suspend fun removeTagFromManga(mangaId: Long, tagId: Long)

    suspend fun removeAllTagsFromManga(mangaId: Long)

    suspend fun setTagsForManga(mangaId: Long, tagIds: List<Long>)

    // Mizu -->
    // Sets tags for multiple manga in a single database transaction, instead of
    // one transaction per manga. Used by bulk-tag operations (e.g. library
    // multi-select) to avoid a separate commit per manga.
    suspend fun setTagsForMangas(mangaTagIds: Map<Long, List<Long>>)
    // Mizu <--
}
