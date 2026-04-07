package eu.kanade.tachiyomi.ui.following

import androidx.activity.compose.BackHandler
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import eu.kanade.presentation.manga.components.ManageTagsDialog
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import eu.kanade.tachiyomi.util.system.toast
import exh.recs.RecommendsScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.util.fastAll
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.core.preference.asState
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.library.DeleteLibraryMangaDialog
import eu.kanade.presentation.library.LibrarySettingsDialog
import eu.kanade.presentation.library.components.LibraryContent
import eu.kanade.presentation.library.components.LibraryToolbar
import eu.kanade.presentation.manga.components.LibraryBottomActionMenu
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.library.LibraryScreenModel
import eu.kanade.tachiyomi.ui.library.LibrarySettingsScreenModel
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import exh.recs.batch.RecommendationSearchBottomSheetDialog
import exh.recs.batch.RecommendationSearchProgressDialog
import exh.recs.batch.SearchStatus
import exh.source.MERGED_SOURCE_ID
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mihon.feature.migration.config.MigrationConfigScreen
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data object FollowingTab : Tab {

    private val queryEvent = Channel<String>()
    private val requestSettingsSheetEvent = Channel<Unit>()

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_updates_enter)
            return TabOptions(
                index = 5u,
                title = "Following",
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        requestSettingsSheetEvent.send(Unit)
    }

    @Composable
    override fun isEnabled(): Boolean {
        val scope = rememberCoroutineScope()
        return remember {
            Injekt.get<UiPreferences>().showNavFollowing().asState(scope)
        }.value
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        val screenModel = rememberScreenModel(tag = "following") {
            LibraryScreenModel(tagFilter = "Following")
        }
        val settingsScreenModel = rememberScreenModel(tag = "following_settings") {
            LibrarySettingsScreenModel()
        }
        val state: LibraryScreenModel.State by screenModel.state.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }

        val onClickRefresh: (Category?) -> Boolean = { category ->
            val started = LibraryUpdateJob.startNow(
                context = context,
                category = if (state.groupType == LibraryGroup.BY_DEFAULT) category else null,
                group = state.groupType,
                groupExtra = when (state.groupType) {
                    LibraryGroup.BY_DEFAULT -> null
                    LibraryGroup.BY_SOURCE, LibraryGroup.BY_TRACK_STATUS -> category?.id?.toString()
                    LibraryGroup.BY_STATUS -> category?.id?.minus(1)?.toString()
                    else -> null
                },
            )
            scope.launch {
                val msgRes = when {
                    !started -> MR.strings.update_already_running
                    category != null -> MR.strings.updating_category
                    else -> MR.strings.updating_library
                }
                snackbarHostState.showSnackbar(context.stringResource(msgRes))
            }
            started
        }

        Scaffold(
            topBar = { scrollBehavior ->
                val title = state.getToolbarTitle(
                    defaultTitle = "Following",
                    defaultCategoryTitle = stringResource(MR.strings.label_default),
                    page = state.coercedActiveCategoryIndex,
                )
                LibraryToolbar(
                    hasActiveFilters = state.hasActiveFilters,
                    selectedCount = state.selection.size,
                    title = title,
                    onClickUnselectAll = screenModel::clearSelection,
                    onClickSelectAll = screenModel::selectAll,
                    onClickInvertSelection = screenModel::invertSelection,
                    onClickFilter = screenModel::showSettingsDialog,
                    onClickRefresh = { onClickRefresh(state.activeCategory) },
                    onClickGlobalUpdate = { onClickRefresh(null) },
                    onClickOpenRandomManga = {
                        scope.launch {
                            val randomItem = screenModel.getRandomLibraryItemForCurrentCategory()
                            if (randomItem != null) {
                                navigator.push(MangaScreen(randomItem.libraryManga.manga.id))
                            } else {
                                snackbarHostState.showSnackbar(
                                    context.stringResource(MR.strings.information_no_entries_found),
                                )
                            }
                        }
                    },
                    onClickSyncNow = {},
                    onClickSyncExh = null,
                    isSyncEnabled = false,
                    isSyncing = false,
                    syncJustFinished = false,
                    syncFailed = false,
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = screenModel::search,
                    scrollBehavior = scrollBehavior.takeIf { !state.showCategoryTabs },
                )
            },
            bottomBar = {
                LibraryBottomActionMenu(
                    visible = state.selectionMode,
                    onChangeCategoryClicked = screenModel::openChangeCategoryDialog,
                    onMarkAsReadClicked = { screenModel.markReadSelection(true) },
                    onMarkAsUnreadClicked = { screenModel.markReadSelection(false) },
                    onDownloadClicked = screenModel::performDownloadAction
                        .takeIf { state.selectedManga.fastAll { !it.isLocal() } },
                    onDeleteClicked = screenModel::openDeleteMangaDialog,
                    onMigrateClicked = {
                        val selection = state.selectedManga
                            .filterNot { it.source == MERGED_SOURCE_ID }
                            .map { it.id }
                        screenModel.clearSelection()
                        if (selection.isNotEmpty()) {
                            navigator.push(MigrationConfigScreen(selection))
                        } else {
                            context.toast(SYMR.strings.no_valid_entry)
                        }
                    },
                    onClickCleanTitles = screenModel::cleanTitles.takeIf { state.showCleanTitles },
                    onClickCollectRecommendations = screenModel::showRecommendationSearchDialog.takeIf { state.selection.size > 1 },
                    onClickAddToMangaDex = null,
                    onClickResetInfo = screenModel::resetInfo.takeIf { state.showResetInfo },
                    onClickManageTags = screenModel::showBulkManageTagsDialog,
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { contentPadding ->
            when {
                state.isLoading -> {
                    LoadingScreen(Modifier.padding(contentPadding))
                }

                state.searchQuery.isNullOrEmpty() && !state.hasActiveFilters && state.isLibraryEmpty -> {
                    EmptyScreen(
                        stringRes = MR.strings.information_empty_library,
                        modifier = Modifier.padding(contentPadding),
                        actions = persistentListOf(),
                    )
                }

                else -> {
                    LibraryContent(
                        categories = state.displayedCategories,
                        searchQuery = state.searchQuery,
                        selection = state.selection,
                        contentPadding = contentPadding,
                        currentPage = state.coercedActiveCategoryIndex,
                        hasActiveFilters = state.hasActiveFilters,
                        showPageTabs = state.showCategoryTabs || !state.searchQuery.isNullOrEmpty(),
                        onChangeCurrentPage = screenModel::updateActiveCategoryIndex,
                        onClickManga = { navigator.push(MangaScreen(it)) },
                        onContinueReadingClicked = { it: LibraryManga ->
                            scope.launchIO {
                                val chapter = screenModel.getNextUnreadChapter(it.manga)
                                if (chapter != null) {
                                    context.startActivity(
                                        ReaderActivity.newIntent(context, chapter.mangaId, chapter.id),
                                    )
                                } else {
                                    snackbarHostState.showSnackbar(context.stringResource(MR.strings.no_next_chapter))
                                }
                            }
                            Unit
                        }.takeIf { state.showMangaContinueButton },
                        onToggleSelection = screenModel::toggleSelection,
                        onToggleRangeSelection = { category, manga ->
                            screenModel.toggleRangeSelection(category, manga)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onRefresh = { onClickRefresh(state.activeCategory) },
                        onGlobalSearchClicked = {
                            navigator.push(GlobalSearchScreen(screenModel.state.value.searchQuery ?: ""))
                        },
                        getItemCountForCategory = { state.getItemCountForCategory(it) },
                        getDisplayMode = { screenModel.getDisplayMode() },
                        getColumnsForOrientation = { screenModel.getColumnsForOrientation(it) },
                        getItemsForCategory = { state.getItemsForCategory(it) },
                    )
                }
            }
        }

        val onDismissRequest = screenModel::closeDialog
        when (val dialog = state.dialog) {
            is LibraryScreenModel.Dialog.SettingsSheet -> {
                LibrarySettingsDialog(
                    onDismissRequest = onDismissRequest,
                    screenModel = settingsScreenModel,
                    category = state.activeCategory,
                    hasCategories = false,
                )
            }
            is LibraryScreenModel.Dialog.ChangeCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = {
                        screenModel.clearSelection()
                        navigator.push(CategoryScreen())
                    },
                    onConfirm = { include, exclude ->
                        screenModel.clearSelection()
                        screenModel.setMangaCategories(
                            mangaList = dialog.manga,
                            addCategories = include,
                            removeCategories = exclude,
                        )
                    },
                )
            }
            is LibraryScreenModel.Dialog.DeleteManga -> {
                DeleteLibraryMangaDialog(
                    containsLocalManga = dialog.manga.any(Manga::isLocal),
                    onDismissRequest = onDismissRequest,
                    onConfirm = { deleteManga, deleteChapter ->
                        screenModel.removeMangas(dialog.manga, deleteManga, deleteChapter)
                        screenModel.clearSelection()
                    },
                )
            }
            is LibraryScreenModel.Dialog.ManageTags -> {
                ManageTagsDialog(
                    allTags = dialog.allTags,
                    mangaTags = persistentListOf(),
                    onDismiss = onDismissRequest,
                    onConfirm = { tagIds ->
                        screenModel.bulkSetTags(tagIds)
                        onDismissRequest()
                    },
                    onCreateTag = { name -> screenModel.createAndAddTag(name) },
                    onDeleteTag = { tagId -> screenModel.deleteTag(tagId) },
                )
            }
            is LibraryScreenModel.Dialog.RecommendationSearchSheet -> {
                RecommendationSearchBottomSheetDialog(
                    onDismissRequest = onDismissRequest,
                    onSearchRequest = {
                        onDismissRequest()
                        screenModel.clearSelection()
                        screenModel.runRecommendationSearch(dialog.manga)
                    },
                )
            }
            else -> {}
        }

        RecommendationSearchProgressDialog(
            status = screenModel.recommendationSearch.status.collectAsState().value,
            setStatusIdle = { screenModel.recommendationSearch.status.value = SearchStatus.Idle },
            setStatusCancelling = { screenModel.recommendationSearch.status.value = SearchStatus.Cancelling },
        )

        BackHandler(enabled = state.selectionMode || state.searchQuery != null) {
            when {
                state.selectionMode -> screenModel.clearSelection()
                state.searchQuery != null -> screenModel.search(null)
            }
        }

        LaunchedEffect(state.selectionMode, state.dialog) {
            HomeScreen.showBottomNav(!state.selectionMode)
        }

        LaunchedEffect(state.isLoading) {
            if (!state.isLoading) {
                (context as? MainActivity)?.ready = true
            }
        }

        val recSearchState by screenModel.recommendationSearch.status.collectAsState()
        LaunchedEffect(recSearchState) {
            when (val current = recSearchState) {
                is SearchStatus.Finished.WithResults -> {
                    RecommendsScreen.Args.MergedSourceMangas(current.results)
                        .let(::RecommendsScreen)
                        .let(navigator::push)
                    screenModel.recommendationSearch.status.value = SearchStatus.Idle
                }
                is SearchStatus.Finished.WithoutResults -> {
                    context.toast(SYMR.strings.rec_no_results)
                    screenModel.recommendationSearch.status.value = SearchStatus.Idle
                }
                is SearchStatus.Cancelling -> {
                    screenModel.cancelRecommendationSearch()
                    screenModel.recommendationSearch.status.value = SearchStatus.Idle
                }
                else -> {}
            }
        }

        LaunchedEffect(Unit) {
            launch { queryEvent.receiveAsFlow().collect(screenModel::search) }
            launch { requestSettingsSheetEvent.receiveAsFlow().collectLatest { screenModel.showSettingsDialog() } }
        }
    }

    suspend fun search(query: String) = queryEvent.send(query)
}
