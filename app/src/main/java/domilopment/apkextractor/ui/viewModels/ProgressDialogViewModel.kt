package domilopment.apkextractor.ui.viewModels

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import domilopment.apkextractor.utils.SingleTimeEvent
import domilopment.apkextractor.R
import domilopment.apkextractor.data.appList.ApplicationModel
import domilopment.apkextractor.data.ProgressDialogUiState
import domilopment.apkextractor.installApk.PackageInstallerSessionCallback
import domilopment.apkextractor.MainActivity
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.PreferenceRepository
import domilopment.apkextractor.utils.settings.ApplicationUtil
import domilopment.apkextractor.utils.ExtractionResult
import domilopment.apkextractor.utils.eventHandler.Event
import domilopment.apkextractor.utils.eventHandler.EventDispatcher
import domilopment.apkextractor.utils.eventHandler.EventType
import domilopment.apkextractor.utils.FileUtil
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ProgressDialogViewModel @Inject constructor(
    application: Application, preferenceRepository: PreferenceRepository
) : AndroidViewModel(application) {
    private val _progressDialogState: MutableStateFlow<ProgressDialogUiState> =
        MutableStateFlow(ProgressDialogUiState())
    val progressDialogState: StateFlow<ProgressDialogUiState> = _progressDialogState.asStateFlow()

    private val _extractionResult: MutableStateFlow<SingleTimeEvent<Triple<String?, ApplicationModel?, Int>>?> =
        MutableStateFlow(null)
    val extractionResult: StateFlow<SingleTimeEvent<Triple<String?, ApplicationModel?, Int>>?> =
        _extractionResult.asStateFlow()

    private val _shareResult: MutableStateFlow<SingleTimeEvent<ArrayList<Uri>?>?> =
        MutableStateFlow(null)
    val shareResult: StateFlow<SingleTimeEvent<ArrayList<Uri>?>?> = _shareResult.asStateFlow()

    val saveDir = preferenceRepository.saveDir
    val appName = preferenceRepository.appSaveName

    private val context get() = getApplication<Application>().applicationContext

    private var runningTask: Job? = null

    /**
     * save multiple apps to filesystem
     * @param list of apps user wants to save
     */
    fun saveApps(list: List<ApplicationModel>) {
        runningTask = viewModelScope.launch {
            val fileUtil = FileUtil(context)
            var application: ApplicationModel? = null
            var errorMessage: String? = null

            _progressDialogState.update {
                it.copy(
                    title = context.getString(R.string.progress_dialog_title_save),
                    tasks = list.size,
                    shouldBeShown = true
                )
            }

            val job = launch extract@{
                list.forEach { app ->
                    application = app
                    withContext(Dispatchers.Main) {
                        _progressDialogState.update { state ->
                            state.copy(
                                process = app.appPackageName, progress = state.progress
                            )
                        }
                    }
                    withContext(Dispatchers.IO) {
                        val newFile = fileUtil.copy(
                            app.appSourceDirectory,
                            saveDir.first()!!,
                            ApplicationUtil.appName(app, appName.first())
                        )
                        when (newFile) {
                            is ExtractionResult.Failure -> errorMessage = newFile.errorMessage
                            is ExtractionResult.Success -> EventDispatcher.emitEvent(
                                Event(
                                    EventType.SAVED, newFile.uri
                                )
                            )
                        }
                    }
                    withContext(Dispatchers.Main) {
                        _progressDialogState.update { state ->
                            state.copy(
                                process = app.appPackageName, progress = state.progress + 1
                            )
                        }
                    }
                    if (errorMessage != null) {
                        this@extract.cancel()
                    }
                }
            }
            job.join()
            _extractionResult.value = SingleTimeEvent(Triple(errorMessage, application, list.size))
        }
    }

    /**
     * create temp files for apps user want to save and get share Uris for them
     * @param list list of all apps
     */
    fun createShareUrisForApps(list: List<ApplicationModel>) {
        runningTask = viewModelScope.launch {
            val files = ArrayList<Uri>()
            val fileUtil = FileUtil(context)
            val jobList = ArrayList<Deferred<Any?>>()

            _progressDialogState.update {
                it.copy(
                    title = context.getString(R.string.progress_dialog_title_share),
                    tasks = list.size,
                    shouldBeShown = true
                )
            }

            list.filter {
                it.isChecked
            }.forEach { app ->
                jobList.add(async {
                    withContext(Dispatchers.IO) {
                        val name = ApplicationUtil.appName(app, appName.first())
                        fileUtil.shareURI(app, name).also {
                            files.add(it)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        _progressDialogState.update { state ->
                            state.copy(
                                progress = state.progress + 1
                            )
                        }
                    }
                })
            }
            jobList.awaitAll()
            _shareResult.value = SingleTimeEvent(files)
        }
    }

    /**
     * Install APK file from ACTION_OPEN_DOCUMENT uri
     * @param apkUri uri from Intent return data
     */
    fun installApk(apkUri: Uri, callback: PackageInstallerSessionCallback) {
        runningTask = viewModelScope.launch(Dispatchers.Main) {
            val packageInstaller = context.applicationContext.packageManager.packageInstaller
            val contentResolver = context.applicationContext.contentResolver
            packageInstaller.registerSessionCallback(callback)

            withContext(Dispatchers.IO) {
                contentResolver.openInputStream(apkUri)?.use { apkStream ->
                    val params =
                        PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                    val sessionId = packageInstaller.createSession(params)
                    callback.initialSessionId = sessionId

                    _progressDialogState.update {
                        it.copy(
                            title = context.getString(R.string.progress_dialog_title_install),
                            process = packageInstaller.getSessionInfo(sessionId)?.appPackageName
                                ?: "",
                            tasks = 100,
                            shouldBeShown = true
                        )
                    }

                    val session = packageInstaller.openSession(sessionId)

                    val length = FileUtil(context).getDocumentInfo(
                        apkUri, DocumentsContract.Document.COLUMN_SIZE
                    )?.size ?: -1

                    session.openWrite("install_apk_session_$sessionId", 0, length)
                        .use { outputStream ->
                            apkStream.copyTo(outputStream)
                            session.fsync(outputStream)
                        }

                    val pendingIntent = Intent(context, MainActivity::class.java).apply {
                        action = MainActivity.PACKAGE_INSTALLATION_ACTION
                    }.let {
                        PendingIntent.getActivity(
                            context, sessionId, it, PendingIntent.FLAG_MUTABLE
                        )
                    }

                    session.commit(pendingIntent.intentSender)
                    session.close()
                }
            }
        }
    }

    /**
     * Update ProgressDialogState for APK installations
     * @param progress current progress of installation
     * @param packageName name of package being installed
     */
    fun updateInstallApkStatus(progress: Float, packageName: String? = "") {
        _progressDialogState.update {
            it.copy(
                title = context.getString(R.string.progress_dialog_title_install),
                process = packageName,
                progress = progress * 100,
            )
        }
    }

    /**
     * Reset Progress dialog state back to default
     */
    fun resetProgress() {
        runningTask?.cancel()
        runningTask = null
        _progressDialogState.value = ProgressDialogUiState()
    }
}