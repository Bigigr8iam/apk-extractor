package domilopment.apkextractor.utils.apkActions

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.vector.ImageVector
import domilopment.apkextractor.R
import domilopment.apkextractor.data.ApplicationModel
import domilopment.apkextractor.utils.MySnackbarVisuals
import domilopment.apkextractor.utils.Utils

enum class ApkActionsOptions(val preferenceValue: String, val title: Int, val icon: ImageVector) {
    SAVE("save_apk", R.string.action_bottom_sheet_save, Icons.Default.Save) {
        override fun getAction(
            context: Context, app: ApplicationModel, params: ApkActionOptionParams
        ) {
            params.saveDir?.let { saveDir ->
                params.callbackFun?.let { showSnackbar ->
                    params.appNameBuilder?.let { appNameBuilder ->
                        ApkActionsManager(context, app).actionSave(
                            saveDir, appNameBuilder, showSnackbar
                        )
                    }
                }
            }
        }

    },
    SHARE("share_apk", R.string.action_bottom_sheet_share, Icons.Default.Share) {
        override fun getAction(
            context: Context, app: ApplicationModel, params: ApkActionOptionParams
        ) {
            params.appNameBuilder?.also { appName ->
                params.shareResult?.also { ApkActionsManager(context, app).actionShare(it, appName) }
            }
        }
    },

    ICON("save_icon", R.string.action_bottom_sheet_save_image, Icons.Default.Image) {
        override fun getAction(
            context: Context, app: ApplicationModel, params: ApkActionOptionParams
        ) {
            params.callbackFun?.let {
                ApkActionsManager(context, app).actionSaveImage(it)
            }
        }
    },
    SETTINGS(
        "open_settings", R.string.action_bottom_sheet_settings, Icons.Default.Settings
    ) {
        override fun getAction(
            context: Context, app: ApplicationModel, params: ApkActionOptionParams
        ) {
            ApkActionsManager(context, app).actionShowSettings()
        }
    },
    OPEN(
        "open_app", R.string.action_bottom_sheet_open, Icons.Default.Android
    ) {
        override fun getAction(
            context: Context, app: ApplicationModel, params: ApkActionOptionParams
        ) {
            ApkActionsManager(context, app).actionOpenApp()
        }
    },
    UNINSTALL(
        "uninstall_app", R.string.action_bottom_sheet_uninstall, Icons.Default.Delete
    ) {
        override fun getAction(
            context: Context, app: ApplicationModel, params: ApkActionOptionParams
        ) {
            params.deleteResult?.let { ApkActionsManager(context, app).actionUninstall(it) }
        }
    };

    abstract fun getAction(
        context: Context, app: ApplicationModel, params: ApkActionOptionParams
    )

    companion object {
        fun isOptionSupported(app: ApplicationModel, action: ApkActionsOptions): Boolean {
            return (action != OPEN || app.launchIntent != null) && (action != UNINSTALL || (!Utils.isSystemApp(
                app
            ) || (app.appUpdateTime > app.appInstallTime)))
        }
    }

    class ApkActionOptionParams private constructor(
        val saveDir: Uri?,
        val appNameBuilder: ((ApplicationModel) -> String)?,
        val callbackFun: ((MySnackbarVisuals) -> Unit)?,
        val shareResult: ActivityResultLauncher<Intent>?,
        val deleteResult: ActivityResultLauncher<Intent>?
    ) {
        data class Builder(
            private var saveDir: Uri? = null,
            private var appNameBuilder: ((ApplicationModel) -> String)? = null,
            private var callbackFun: ((MySnackbarVisuals) -> Unit)? = null,
            private var shareResult: ActivityResultLauncher<Intent>? = null,
            private var deleteResult: ActivityResultLauncher<Intent>? = null
        ) {
            fun setSaveDir(uri: Uri) = apply { this.saveDir = uri }

            fun setAppNameBuilder(appNameBuilder: (ApplicationModel) -> String) =
                apply { this.appNameBuilder = appNameBuilder }

            fun setCallbackFun(showSnackbar: (MySnackbarVisuals) -> Unit) =
                apply { this.callbackFun = showSnackbar }

            fun setShareResult(activityResultLauncher: ActivityResultLauncher<Intent>) =
                apply { this.shareResult = activityResultLauncher }

            fun setDeleteResult(activityResultLauncher: ActivityResultLauncher<Intent>) =
                apply { this.deleteResult = activityResultLauncher }

            fun build() = ApkActionOptionParams(
                saveDir, appNameBuilder, callbackFun, shareResult, deleteResult
            )
        }
    }
}