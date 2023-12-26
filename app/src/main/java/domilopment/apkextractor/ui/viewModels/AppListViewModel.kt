package domilopment.apkextractor.ui.viewModels

import android.app.Application
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import domilopment.apkextractor.data.*
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.PreferenceRepository
import domilopment.apkextractor.dependencyInjection.applications.ApplicationRepository
import domilopment.apkextractor.utils.settings.ApplicationUtil
import domilopment.apkextractor.utils.eventHandler.Event
import domilopment.apkextractor.utils.eventHandler.EventDispatcher
import domilopment.apkextractor.utils.eventHandler.EventType
import domilopment.apkextractor.utils.eventHandler.Observer
import domilopment.apkextractor.utils.Utils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
@OptIn(FlowPreview::class)
class AppListViewModel @Inject constructor(
    application: Application,
    private val preferenceRepository: PreferenceRepository,
    private val appsRepository: ApplicationRepository
) : AndroidViewModel(application), Observer {
    override val key: String = "AppListViewModel"

    private val _mainFragmentState: MutableStateFlow<AppListScreenState> =
        MutableStateFlow(AppListScreenState())
    val mainFragmentState: StateFlow<AppListScreenState> = _mainFragmentState.asStateFlow()

    private val _searchQuery: MutableStateFlow<String?> = MutableStateFlow(null)

    private val appListFavorites = preferenceRepository.appListFavorites
    val saveDir = preferenceRepository.saveDir
    val appName = preferenceRepository.appSaveName
    val appSortOrder = preferenceRepository.appSortOrder
    val appSortFavorites = preferenceRepository.appSortFavorites
    val appSortAsc = preferenceRepository.appSortAsc
    val updatedSystemApps = preferenceRepository.updatedSysApps
    val systemApps = preferenceRepository.sysApps
    val userApps = preferenceRepository.userApps
    val filterInstaller = preferenceRepository.appFilterInstaller
    val filterCategory = preferenceRepository.appFilterCategory
    val filterOthers = preferenceRepository.appFilterOthers
    val rightSwipeAction = preferenceRepository.appRightSwipeAction
    val leftSwipeAction = preferenceRepository.appLeftSwipeAction

    private val context get() = getApplication<Application>().applicationContext

    init {
        // Set applications in view once they are loaded
        viewModelScope.launch {
            _searchQuery.debounce(500L).combine(combine(
                combine(
                    combine(
                        appsRepository.apps,
                        updatedSystemApps,
                        systemApps,
                        userApps,
                        appListFavorites
                    ) { appList, updatedSysApps, sysApps, userApps, favorites ->
                        ApplicationUtil.selectedAppTypes(
                            appList, updatedSysApps, sysApps, userApps, favorites
                        )
                    }, filterInstaller, filterCategory, filterOthers
                ) { appList, installer, category, others ->
                    ApplicationUtil.filterApps(appList, installer, category, others)
                }, appSortOrder, appSortFavorites, appSortAsc
            ) { appList, sortMode, sortFavorites, sortAsc ->
                ApplicationUtil.sortAppData(appList, sortMode.ordinal, sortFavorites, sortAsc)
            }) { searchQuery, appList ->
                val searchString = searchQuery?.trim()

                return@combine if (searchString.isNullOrBlank()) {
                    appList
                } else {
                    appList.filter {
                        it.appName.contains(
                            searchString, ignoreCase = true
                        ) || it.appPackageName.contains(
                            searchString, ignoreCase = true
                        )
                    }
                }
            }.collect { appList ->
                _mainFragmentState.update { state ->
                    state.copy(appList = appList,
                        isRefreshing = false,
                        selectedApp = state.selectedApp?.let { sa ->
                            appList.find {
                                it.appPackageName == sa.appPackageName
                            }
                        })
                }
            }
        }

        EventDispatcher.registerObserver(this, EventType.INSTALLED, EventType.UNINSTALLED)
    }

    override fun onCleared() {
        EventDispatcher.unregisterObserver(this, EventType.ANY)
        super.onCleared()
    }

    override fun onEventReceived(event: Event<*>) {
        when (event.eventType) {
            EventType.INSTALLED -> addApps(event.data as String)
            EventType.UNINSTALLED -> if (Utils.isPackageInstalled(
                    context.packageManager, event.data as String
                )
            ) uninstallApps(event.data)

            else -> return
        }
    }

    /**
     * Select a specific Application from list in view
     * and set it in BottomSheet state
     * @param app selected application
     */
    fun selectApplication(app: ApplicationModel?) {
        _mainFragmentState.update { state ->
            state.copy(selectedApp = app)
        }
    }

    /**
     * Set query string from search in App List, sets empty string if null
     * @param query last input Search String
     */
    fun searchQuery(query: String?) {
        _searchQuery.value = query
    }

    /**
     * Update App list
     */
    fun updateApps() {
        _mainFragmentState.update {
            it.copy(isRefreshing = true)
        }
        viewModelScope.launch {
            async { appsRepository.updateApps() }
        }
    }

    fun addApps(packageName: String) {
        viewModelScope.launch {
            async { appsRepository.addApp(ApplicationModel(context.packageManager, packageName)) }
        }
    }

    fun uninstallApps(app: ApplicationModel) {
        if (Utils.isPackageInstalled(context.packageManager, app.appPackageName)) return

        _mainFragmentState.update { state ->
            state.copy(
                isRefreshing = true,
                appList = state.appList.filter { it.appPackageName != app.appPackageName },
                selectedApp = if (state.selectedApp?.appPackageName == app.appPackageName) null else state.selectedApp
            )
        }
        viewModelScope.launch {
            async { appsRepository.removeApp(app) }
        }
    }

    private fun uninstallApps(packageName: String) {
        _mainFragmentState.value.appList.find { it.appPackageName == packageName }?.let {
            uninstallApps(it)
        }
    }

    /**
     * update State for List view inside UI once user changed selected app types inside the Settings
     * @param key key of switch preference who has changed
     * @param b true for should be selected, false if no longer should be included
     * @throws Exception if key is not "updated_system_apps", "system_apps" or "user_apps"
     */
    @Throws(Exception::class)
    fun changeSelection(key: String, b: Boolean) {
        _mainFragmentState.update { state ->
            state.copy(isRefreshing = true)
        }
        viewModelScope.launch {
            when (key) {
                "updated_system_apps" -> preferenceRepository.setUpdatedSysApps(b)
                "system_apps" -> preferenceRepository.setSysApps(b)
                "user_apps" -> preferenceRepository.setUserApps(b)
                else -> throw Exception("No available key provided!")
            }
        }
    }

    fun updateApp(app: ApplicationModel) {
        _mainFragmentState.update { state ->
            state.copy(appList = state.appList.toMutableList()
                .map { if (it.appPackageName == app.appPackageName) app else it })
        }

    }

    fun selectAllApps(select: Boolean) {
        _mainFragmentState.update { state ->
            state.copy(appList = state.appList.toMutableList().map { it.copy(isChecked = select) })
        }
    }

    fun setSortAsc(b: Boolean) {
        viewModelScope.launch { preferenceRepository.setAppSortAsc(b) }
    }

    fun setSortOrder(value: Int) {
        viewModelScope.launch { preferenceRepository.setAppSortOrder(value) }
    }

    fun setSortFavorites(b: Boolean) {
        viewModelScope.launch { preferenceRepository.setAppSortFavorites(b) }
    }

    fun setInstallationSource(value: String?) {
        viewModelScope.launch { preferenceRepository.setAppFilterInstaller(value) }
    }

    fun setCategory(s: String?) {
        viewModelScope.launch { preferenceRepository.setAppFilterCategory(s) }
    }

    fun setOtherFilter(strings: Set<String>) {
        viewModelScope.launch { preferenceRepository.setAppFilterOthers(strings) }
    }

    fun editFavorites(appPackageName: String, checked: Boolean) {
        viewModelScope.launch {
            ApplicationUtil.editFavorites(appListFavorites.first(), appPackageName, checked).let {
                preferenceRepository.setAppListFavorites(it)
            }

        }
    }
}