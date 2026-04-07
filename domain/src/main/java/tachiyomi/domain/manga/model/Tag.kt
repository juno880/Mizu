package tachiyomi.domain.manga.model

import androidx.compose.runtime.Immutable

@Immutable
data class Tag(
    val id: Long,
    val name: String,
)
