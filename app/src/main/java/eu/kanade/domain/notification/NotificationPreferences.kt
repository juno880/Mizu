package eu.kanade.domain.notification

import tachiyomi.core.common.preference.PreferenceStore

class NotificationPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun showNewChaptersNotification() = preferenceStore.getBoolean("notif_new_chapters", true)
    fun showLibraryProgressNotification() = preferenceStore.getBoolean("notif_library_progress", true)
    fun showLibraryErrorNotification() = preferenceStore.getBoolean("notif_library_error", true)
    fun showDownloadProgressNotification() = preferenceStore.getBoolean("notif_download_progress", true)
    fun showDownloadErrorNotification() = preferenceStore.getBoolean("notif_download_error", true)
    fun showBackupRestoreNotification() = preferenceStore.getBoolean("notif_backup_restore", true)
    fun showExtensionUpdateNotification() = preferenceStore.getBoolean("notif_extension_updates", true)
    fun showSyncNotification() = preferenceStore.getBoolean("notif_sync", true)
}
