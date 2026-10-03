package eu.kanade.tachiyomi.data.coil

import android.net.Uri

/**
 * Coil fetch model for a chapter thumbnail pulled from the first image inside
 * a local .cbz/.zip chapter file, identified by its content [uri].
 */
data class CbzCoverData(val uri: Uri)
