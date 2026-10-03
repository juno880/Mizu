package eu.kanade.presentation.manga.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import eu.kanade.tachiyomi.data.coil.CbzCoverData
import eu.kanade.tachiyomi.data.download.model.Download
import me.saket.swipe.SwipeableActionsBox
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.SECONDARY_ALPHA
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.selectedBackground
import tachiyomi.domain.manga.model.MangaCover as MangaCoverModel

/**
 * Same row layout as [MangaChapterListItem], with a cover thumbnail added at the
 * start. Used instead of [MangaChapterListItem] when the manga has
 * `showChapterThumbnails` enabled.
 *
 * The thumbnail is resolved from [chapterUrl]: for HTTP sources (e.g. Komga),
 * it reuses the existing authenticated cover-fetching pathway pointed at
 * `$chapterUrl/thumbnail`. For local chapters, it reads the first image out of
 * the chapter's own .cbz file via [uri] and [CbzCoverData].
 */
@Composable
fun MangaVolumeListItem(
    title: String,
    date: String?,
    readProgress: String?,
    scanlator: String?,
    sourceName: String?,
    read: Boolean,
    bookmark: Boolean,
    selected: Boolean,
    downloadIndicatorEnabled: Boolean,
    downloadStateProvider: () -> Download.State,
    downloadProgressProvider: () -> Int,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onDownloadClick: ((ChapterDownloadAction) -> Unit)?,
    onChapterSwipe: (LibraryPreferences.ChapterSwipeAction) -> Unit,
    // Mizu -->
    mangaId: Long,
    sourceId: Long,
    chapterUrl: String,
    uri: Uri?,
    thumbnailSize: Int,
    // Mizu <--
    modifier: Modifier = Modifier,
) {
    val start = getSwipeAction(
        action = chapterSwipeStartAction,
        read = read,
        bookmark = bookmark,
        downloadState = downloadStateProvider(),
        background = MaterialTheme.colorScheme.primaryContainer,
        onSwipe = { onChapterSwipe(chapterSwipeStartAction) },
    )
    val end = getSwipeAction(
        action = chapterSwipeEndAction,
        read = read,
        bookmark = bookmark,
        downloadState = downloadStateProvider(),
        background = MaterialTheme.colorScheme.primaryContainer,
        onSwipe = { onChapterSwipe(chapterSwipeEndAction) },
    )

    // Mizu -->
    val thumbnailModel = remember(chapterUrl, uri) {
        when {
            chapterUrl.startsWith("http") -> MangaCoverModel(
                mangaId = mangaId,
                sourceId = sourceId,
                isMangaFavorite = false,
                ogUrl = "$chapterUrl/thumbnail",
                lastModified = 0L,
            )
            uri != null -> {
                val parts = chapterUrl.split("/")
                if (parts.size >= 2) {
                    val fileUri = Uri.withAppendedPath(Uri.withAppendedPath(uri, parts[0]), parts[1])
                    CbzCoverData(fileUri)
                } else {
                    null
                }
            }
            else -> null
        }
    }
    val thumbnailHeight = ((thumbnailSize * 18) + 40).dp
    // Mizu <--

    SwipeableActionsBox(
        modifier = Modifier.clipToBounds(),
        startActions = listOfNotNull(start),
        endActions = listOfNotNull(end),
        swipeThreshold = swipeActionThreshold,
        backgroundUntilSwipeThreshold = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = modifier
                .selectedBackground(selected)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .padding(start = 16.dp, top = 0.dp, end = 8.dp, bottom = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Mizu -->
            // A plain AsyncImage with no width constraint measures at the
            // image's full intrinsic pixel size, which can blow out the Row
            // and push the text out of view. Instead, read the real aspect
            // ratio from the painter once available (falling back to a
            // standard book ratio before it loads) and apply that via
            // aspectRatio(), so the composable always has a bounded,
            // well-defined width during layout.
            val painter = rememberAsyncImagePainter(
                model = thumbnailModel,
                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
                error = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentScale = ContentScale.Fit,
            )
            val intrinsicSize = painter.intrinsicSize
            val aspectRatio = if (intrinsicSize.isSpecified && intrinsicSize.height > 0f) {
                intrinsicSize.width / intrinsicSize.height
            } else {
                2f / 3f
            }
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .height(thumbnailHeight)
                    .aspectRatio(aspectRatio)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .padding(end = 12.dp),
                contentScale = ContentScale.Fit,
            )
            // Mizu <--

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    var textHeight by remember { mutableIntStateOf(0) }
                    if (!read) {
                        Icon(
                            imageVector = Icons.Filled.Circle,
                            contentDescription = stringResource(MR.strings.unread),
                            modifier = Modifier
                                .height(8.dp)
                                .padding(end = 4.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (bookmark) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = stringResource(MR.strings.action_filter_bookmarked),
                            modifier = Modifier
                                .sizeIn(maxHeight = with(LocalDensity.current) { textHeight.toDp() - 2.dp }),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { textHeight = it.size.height },
                        color = LocalContentColor.current.copy(alpha = if (read) DISABLED_ALPHA else 1f),
                    )
                }

                Row {
                    val subtitleStyle = MaterialTheme.typography.bodySmall
                        .merge(
                            color = LocalContentColor.current
                                .copy(alpha = if (read) DISABLED_ALPHA else SECONDARY_ALPHA),
                        )
                    ProvideTextStyle(value = subtitleStyle) {
                        if (date != null) {
                            Text(
                                text = date,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (readProgress != null || scanlator != null || sourceName != null) {
                                DotSeparatorText()
                            }
                        }
                        if (readProgress != null) {
                            Text(
                                text = readProgress,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = LocalContentColor.current.copy(alpha = DISABLED_ALPHA),
                            )
                            if (scanlator != null || sourceName != null) DotSeparatorText()
                        }
                        if (sourceName != null) {
                            Text(
                                text = sourceName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (scanlator != null) DotSeparatorText()
                        }
                        if (scanlator != null) {
                            Text(
                                text = scanlator,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            ChapterDownloadIndicator(
                enabled = downloadIndicatorEnabled,
                modifier = Modifier.padding(start = 4.dp),
                downloadStateProvider = downloadStateProvider,
                downloadProgressProvider = downloadProgressProvider,
                onClick = { onDownloadClick?.invoke(it) },
            )
        }
    }
}
