package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import eu.kanade.domain.notification.NotificationPreferences
import eu.kanade.presentation.more.settings.Preference
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsNotificationsScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_notifications

    @Composable
    override fun getPreferences(): List<Preference> {
        val notificationPreferences = remember { Injekt.get<NotificationPreferences>() }
        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.label_library),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = notificationPreferences.showNewChaptersNotification(),
                        title = stringResource(MR.strings.channel_new_chapters),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = notificationPreferences.showLibraryProgressNotification(),
                        title = stringResource(MR.strings.channel_progress),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = notificationPreferences.showLibraryErrorNotification(),
                        title = stringResource(MR.strings.channel_errors),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.download_notifier_downloader_title),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = notificationPreferences.showDownloadProgressNotification(),
                        title = stringResource(MR.strings.channel_progress),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = notificationPreferences.showDownloadErrorNotification(),
                        title = stringResource(MR.strings.channel_errors),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.label_backup),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = notificationPreferences.showBackupRestoreNotification(),
                        title = stringResource(MR.strings.channel_progress),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.label_recent_updates),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = notificationPreferences.showExtensionUpdateNotification(),
                        title = stringResource(MR.strings.channel_ext_updates),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.syncing_library),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = notificationPreferences.showSyncNotification(),
                        title = stringResource(MR.strings.syncing_library),
                    ),
                ),
            ),
        )
    }
}