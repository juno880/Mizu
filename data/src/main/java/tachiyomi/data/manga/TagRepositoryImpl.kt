package tachiyomi.data.manga

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.manga.model.Tag
import tachiyomi.domain.manga.repository.TagRepository

class TagRepositoryImpl(
    private val handler: DatabaseHandler,
) : TagRepository {

    override suspend fun getAllTags(): List<Tag> {
        return handler.awaitList { tagQueries.getAllTags(::mapTag) }
    }

    override fun getAllTagsAsFlow(): Flow<List<Tag>> {
        return handler.subscribeToList { tagQueries.getAllTags(::mapTag) }
    }

    override suspend fun getTagsForManga(mangaId: Long): List<Tag> {
        return handler.awaitList { tagQueries.getTagsForManga(mangaId, ::mapTag) }
    }

    override fun getTagsForMangaAsFlow(mangaId: Long): Flow<List<Tag>> {
        return handler.subscribeToList { tagQueries.getTagsForManga(mangaId, ::mapTag) }
    }

    override suspend fun getMangaIdsForTag(tagId: Long): List<Long> {
        return handler.awaitList { tagQueries.getMangaIdsForTag(tagId) }
    }

    override suspend fun insertTag(name: String): Long {
        return handler.await {
            tagQueries.insertTag(name)
            tagQueries.selectLastInsertedRowId().executeAsOne()
        }
    }

    override suspend fun deleteTag(tagId: Long) {
        handler.await { tagQueries.deleteTag(tagId) }
    }

    override suspend fun renameTag(tagId: Long, name: String) {
        handler.await { tagQueries.renameTag(name, tagId) }
    }

    override suspend fun addTagToManga(mangaId: Long, tagId: Long) {
        handler.await { tagQueries.addMangaTag(mangaId, tagId) }
    }

    override suspend fun removeTagFromManga(mangaId: Long, tagId: Long) {
        handler.await { tagQueries.removeMangaTag(mangaId, tagId) }
    }

    override suspend fun removeAllTagsFromManga(mangaId: Long) {
        handler.await { tagQueries.removeAllTagsFromManga(mangaId) }
    }

    override suspend fun setTagsForManga(mangaId: Long, tagIds: List<Long>) {
        handler.await(inTransaction = true) {
            tagQueries.removeAllTagsFromManga(mangaId)
            tagIds.forEach { tagId ->
                tagQueries.addMangaTag(mangaId, tagId)
            }
        }
    }

    private fun mapTag(id: Long, name: String): Tag {
        return Tag(id = id, name = name)
    }
}
