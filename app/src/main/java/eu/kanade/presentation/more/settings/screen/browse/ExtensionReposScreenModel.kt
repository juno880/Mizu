package eu.kanade.presentation.more.settings.screen.browse

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import mihon.domain.extensionrepo.interactor.CreateExtensionRepo
import mihon.domain.extensionrepo.interactor.DeleteExtensionRepo
import mihon.domain.extensionrepo.interactor.GetExtensionRepo
import mihon.domain.extensionrepo.interactor.ReplaceExtensionRepo
import mihon.domain.extensionrepo.interactor.UpdateExtensionRepo
import mihon.domain.extensionrepo.model.ExtensionRepo
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ExtensionReposScreenModel(
    private val getExtensionRepo: GetExtensionRepo = Injekt.get(),
    private val createExtensionRepo: CreateExtensionRepo = Injekt.get(),
    private val deleteExtensionRepo: DeleteExtensionRepo = Injekt.get(),
    private val replaceExtensionRepo: ReplaceExtensionRepo = Injekt.get(),
    private val updateExtensionRepo: UpdateExtensionRepo = Injekt.get(),
    private val extensionManager: ExtensionManager = Injekt.get(),
    private val sourcePreferences: SourcePreferences = Injekt.get(),
) : StateScreenModel<RepoScreenState>(RepoScreenState.Loading) {

    private val _events: Channel<RepoEvent> = Channel(Int.MAX_VALUE)
    val events = _events.receiveAsFlow()

    init {
        screenModelScope.launchIO {
            combine(
                getExtensionRepo.subscribeAll(),
                sourcePreferences.disabledRepos().changes(),
            ) { repos, disabledRepos ->
                RepoScreenState.Success(
                    repos = repos.toImmutableSet(),
                    disabledRepos = disabledRepos.toImmutableSet(),
                )
            }.collectLatest { state ->
                mutableState.update { state }
            }
        }
    }

    fun createRepo(baseUrl: String) {
        screenModelScope.launchIO {
            when (val result = createExtensionRepo.await(baseUrl)) {
                CreateExtensionRepo.Result.Success -> extensionManager.findAvailableExtensions()
                CreateExtensionRepo.Result.InvalidUrl -> _events.send(RepoEvent.InvalidUrl)
                CreateExtensionRepo.Result.RepoAlreadyExists -> _events.send(RepoEvent.RepoAlreadyExists)
                is CreateExtensionRepo.Result.DuplicateFingerprint -> {
                    showDialog(RepoDialog.Conflict(result.oldRepo, result.newRepo))
                }
                else -> {}
            }
        }
    }

    fun replaceRepo(newRepo: ExtensionRepo) {
        screenModelScope.launchIO {
            replaceExtensionRepo.await(newRepo)
        }
    }

    fun refreshRepos() {
        val status = state.value
        if (status is RepoScreenState.Success) {
            screenModelScope.launchIO {
                updateExtensionRepo.awaitAll()
            }
        }
    }

    fun deleteRepo(baseUrl: String) {
        screenModelScope.launchIO {
            deleteExtensionRepo.await(baseUrl)
            extensionManager.findAvailableExtensions()
        }
    }

    fun toggleRepo(baseUrl: String, enabled: Boolean) {
        screenModelScope.launchIO {
            val disabled = sourcePreferences.disabledRepos().get().toMutableSet()
            if (enabled) {
                disabled.remove(baseUrl)
            } else {
                disabled.add(baseUrl)
            }
            sourcePreferences.disabledRepos().set(disabled)
            extensionManager.findAvailableExtensions()
        }
    }

    fun showDialog(dialog: RepoDialog) {
        mutableState.update {
            when (it) {
                RepoScreenState.Loading -> it
                is RepoScreenState.Success -> it.copy(dialog = dialog)
            }
        }
    }

    fun dismissDialog() {
        mutableState.update {
            when (it) {
                RepoScreenState.Loading -> it
                is RepoScreenState.Success -> it.copy(dialog = null)
            }
        }
    }
}

sealed class RepoEvent {
    sealed class LocalizedMessage(val stringRes: StringResource) : RepoEvent()
    data object InvalidUrl : LocalizedMessage(MR.strings.invalid_repo_name)
    data object RepoAlreadyExists : LocalizedMessage(MR.strings.error_repo_exists)
}

sealed class RepoDialog {
    data object Create : RepoDialog()
    data class Delete(val repo: String) : RepoDialog()
    data class Conflict(val oldRepo: ExtensionRepo, val newRepo: ExtensionRepo) : RepoDialog()
    data class Confirm(val url: String) : RepoDialog()
}

sealed class RepoScreenState {

    @Immutable
    data object Loading : RepoScreenState()

    @Immutable
    data class Success(
        val repos: ImmutableSet<ExtensionRepo>,
        val disabledRepos: ImmutableSet<String> = kotlinx.collections.immutable.persistentSetOf(),
        val oldRepos: ImmutableSet<String>? = null,
        val dialog: RepoDialog? = null,
    ) : RepoScreenState() {

        val isEmpty: Boolean
            get() = repos.isEmpty()
    }
}
