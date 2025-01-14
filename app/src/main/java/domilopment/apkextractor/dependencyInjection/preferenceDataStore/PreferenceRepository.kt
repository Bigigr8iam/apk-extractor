package domilopment.apkextractor.dependencyInjection.preferenceDataStore

import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.APK_SORT_ORDER
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.APP_FILTER_CATEGORY
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.APP_FILTER_INSTALLER
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.APP_FILTER_OTHERS
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.APP_LEFT_SWIPE_ACTION
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.APP_LIST_FAVORITES
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.APP_RIGHT_SWIPE_ACTION
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.APP_SAVE_NAME
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.APP_SORT_ASC
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.APP_SORT_FAVORITES
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.APP_SORT_ORDER
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.APP_AUTO_BACKUP_LIST
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.APP_SWIPE_ACTION_CUSTOM_THRESHOLD
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.APP_SWIPE_ACTION_THRESHOLD_MODIFIER
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.AUTO_BACKUP_SERVICE
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.CHECK_UPDATE_ON_START
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.MATERIAL_YOU
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.NIGHT_MODE
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.SAVE_DIR
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.SYSTEM_APPS
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.UPDATED_SYSTEM_APPS
import domilopment.apkextractor.dependencyInjection.preferenceDataStore.MyPreferenceRepository.PreferencesKeys.USER_APPS
import domilopment.apkextractor.utils.Constants
import domilopment.apkextractor.utils.apkActions.ApkActionsOptions
import domilopment.apkextractor.utils.settings.ApkSortOptions
import domilopment.apkextractor.utils.settings.AppSortOptions
import domilopment.apkextractor.utils.settings.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

interface PreferenceRepository {
    val saveDir: Flow<Uri?>
    suspend fun setSaveDir(uri: Uri)

    val updatedSysApps: Flow<Boolean>
    suspend fun setUpdatedSysApps(value: Boolean)

    val sysApps: Flow<Boolean>
    suspend fun setSysApps(value: Boolean)

    val userApps: Flow<Boolean>
    suspend fun setUserApps(value: Boolean)

    val appSortOrder: Flow<AppSortOptions>
    suspend fun setAppSortOrder(value: Int)

    val appSortFavorites: Flow<Boolean>
    suspend fun setAppSortFavorites(value: Boolean)

    val appSortAsc: Flow<Boolean>
    suspend fun setAppSortAsc(value: Boolean)

    val appListFavorites: Flow<Set<String>>
    suspend fun setAppListFavorites(favorites: Set<String>)

    val appFilterInstaller: Flow<String?>
    suspend fun setAppFilterInstaller(value: String?)

    val appFilterCategory: Flow<String?>
    suspend fun setAppFilterCategory(value: String?)

    val appFilterOthers: Flow<Set<String>>
    suspend fun setAppFilterOthers(value: Set<String>)

    val autoBackupAppList: Flow<Set<String>>
    suspend fun setListOfAutoBackupApps(list: Set<String>)

    val appSaveName: Flow<Set<String>>
    suspend fun setAppSaveName(set: Set<String>)

    val apkSortOrder: Flow<ApkSortOptions>
    suspend fun setApkSortOrder(value: String)

    val checkUpdateOnStart: Flow<Boolean>
    suspend fun setCheckUpdateOnStart(value: Boolean)

    val appRightSwipeAction: Flow<ApkActionsOptions>
    suspend fun setRightSwipeAction(value: String)

    val appLeftSwipeAction: Flow<ApkActionsOptions>
    suspend fun setLeftSwipeAction(value: String)

    val appSwipeActionCustomThreshold: Flow<Boolean>
    suspend fun setSwipeActionCustomThreshold(value: Boolean)

    val appSwipeActionThresholdMod: Flow<Float>
    suspend fun setSwipeActionThresholdMod(value: Float)

    val autoBackupService: Flow<Boolean>
    suspend fun setAutoBackupService(value: Boolean)

    val useMaterialYou: Flow<Boolean>
    suspend fun setUseMaterialYou(value: Boolean)

    val nightMode: Flow<Int>
    suspend fun setNightMode(value: Int)
}

class MyPreferenceRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferenceRepository {
    private object PreferencesKeys {
        val SAVE_DIR = stringPreferencesKey(Constants.PREFERENCE_KEY_SAVE_DIR)
        val CHECK_UPDATE_ON_START =
            booleanPreferencesKey(Constants.PREFERENCE_CHECK_UPDATE_ON_START)
        val UPDATED_SYSTEM_APPS =
            booleanPreferencesKey(Constants.PREFERENCE_KEY_UPDATED_SYSTEM_APPS)
        val SYSTEM_APPS = booleanPreferencesKey(Constants.PREFERENCE_KEY_SYSTEM_APPS)
        val USER_APPS = booleanPreferencesKey(Constants.PREFERENCE_KEY_USER_APPS)
        val APP_SORT_ORDER = intPreferencesKey(Constants.PREFERENCE_KEY_APP_SORT)
        val APP_SORT_FAVORITES = booleanPreferencesKey(Constants.PREFERENCE_KEY_SORT_FAVORITES)
        val APP_SORT_ASC = booleanPreferencesKey(Constants.PREFERENCE_KEY_APP_SORT_ASC)
        val APP_LIST_FAVORITES = stringSetPreferencesKey(Constants.PREFERENCE_KEY_FAVORITES)
        val APP_FILTER_INSTALLER = stringPreferencesKey(Constants.PREFERENCE_KEY_FILTER_INSTALLER)
        val APP_FILTER_CATEGORY = stringPreferencesKey(Constants.PREFERENCE_KEY_FILTER_CATEGORY)
        val APP_FILTER_OTHERS = stringSetPreferencesKey(Constants.PREFERENCE_KEY_FILTER_OTHERS)
        val APP_RIGHT_SWIPE_ACTION = stringPreferencesKey("list_preference_swipe_actions_right")
        val APP_LEFT_SWIPE_ACTION = stringPreferencesKey("list_preference_swipe_actions_left")
        val APP_SWIPE_ACTION_CUSTOM_THRESHOLD =
            booleanPreferencesKey("swipe_action_custom_threshold")
        val APP_SWIPE_ACTION_THRESHOLD_MODIFIER =
            floatPreferencesKey("swipe_action_threshold_modifier")
        val APP_AUTO_BACKUP_LIST = stringSetPreferencesKey("app_list_auto_backup")
        val APP_SAVE_NAME = stringSetPreferencesKey("app_save_name")
        val APK_SORT_ORDER = stringPreferencesKey("apk_sort")
        val AUTO_BACKUP_SERVICE = booleanPreferencesKey("auto_backup")
        val MATERIAL_YOU = booleanPreferencesKey("use_material_you")
        val NIGHT_MODE = stringPreferencesKey("list_preference_ui_mode")
    }

    private fun <T> getPreference(key: Preferences.Key<T>): Flow<T?> =
        dataStore.data.catch { exception ->
            /*
             * dataStore.data throws an IOException when an error
             * is encountered when reading data
             */
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[key]
        }

    private suspend fun <T> setPreference(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    override val saveDir: Flow<Uri?> = getPreference(SAVE_DIR).map { it?.let { Uri.parse(it) } }
    override suspend fun setSaveDir(uri: Uri) = setPreference(SAVE_DIR, uri.toString())

    override val updatedSysApps: Flow<Boolean> =
        getPreference(UPDATED_SYSTEM_APPS).map { it ?: false }

    override suspend fun setUpdatedSysApps(value: Boolean) =
        setPreference(UPDATED_SYSTEM_APPS, value)

    override val sysApps: Flow<Boolean> = getPreference(SYSTEM_APPS).map { it ?: false }

    override suspend fun setSysApps(value: Boolean) = setPreference(SYSTEM_APPS, value)

    override val userApps: Flow<Boolean> = getPreference(USER_APPS).map { it ?: true }

    override suspend fun setUserApps(value: Boolean) = setPreference(USER_APPS, value)

    override val appSortOrder: Flow<AppSortOptions> = getPreference(APP_SORT_ORDER).map {
        it?.let { AppSortOptions[it] } ?: AppSortOptions.SORT_BY_NAME
    }

    override suspend fun setAppSortOrder(value: Int) = setPreference(APP_SORT_ORDER, value)

    override val appSortFavorites: Flow<Boolean> =
        getPreference(APP_SORT_FAVORITES).map { it ?: true }

    override suspend fun setAppSortFavorites(value: Boolean) =
        setPreference(APP_SORT_FAVORITES, value)

    override val appSortAsc: Flow<Boolean> = getPreference(APP_SORT_ASC).map { it ?: true }
    override suspend fun setAppSortAsc(value: Boolean) = setPreference(APP_SORT_ASC, value)

    override val appListFavorites: Flow<Set<String>> =
        getPreference(APP_LIST_FAVORITES).map { it ?: emptySet() }

    override suspend fun setAppListFavorites(favorites: Set<String>) =
        setPreference(APP_LIST_FAVORITES, favorites)

    override val appFilterInstaller: Flow<String?> = getPreference(APP_FILTER_INSTALLER)

    override suspend fun setAppFilterInstaller(value: String?) {
        if (value.isNullOrEmpty()) dataStore.edit { preferences ->
            preferences.remove(
                APP_FILTER_INSTALLER
            )
        }
        else setPreference(APP_FILTER_INSTALLER, value)
    }

    override val appFilterCategory: Flow<String?> = getPreference(APP_FILTER_CATEGORY)

    override suspend fun setAppFilterCategory(value: String?) {
        if (value.isNullOrEmpty()) dataStore.edit { preferences ->
            preferences.remove(
                APP_FILTER_CATEGORY
            )
        }
        else setPreference(APP_FILTER_CATEGORY, value)
    }

    override val appFilterOthers: Flow<Set<String>> =
        getPreference(APP_FILTER_OTHERS).map { it ?: emptySet() }

    override suspend fun setAppFilterOthers(value: Set<String>) =
        setPreference(APP_FILTER_OTHERS, value)

    override val autoBackupAppList: Flow<Set<String>> =
        getPreference(APP_AUTO_BACKUP_LIST).map { it ?: emptySet() }

    override suspend fun setListOfAutoBackupApps(list: Set<String>) =
        setPreference(APP_AUTO_BACKUP_LIST, list)

    override val appSaveName: Flow<Set<String>> =
        getPreference(APP_SAVE_NAME).map { it ?: setOf("0:name") }

    override suspend fun setAppSaveName(set: Set<String>) = setPreference(APP_SAVE_NAME, set)

    override val apkSortOrder: Flow<ApkSortOptions> = getPreference(APK_SORT_ORDER).map {
        it?.let { ApkSortOptions[it] } ?: ApkSortOptions.SORT_BY_FILE_SIZE_DESC
    }

    override suspend fun setApkSortOrder(value: String) = setPreference(APK_SORT_ORDER, value)

    override val checkUpdateOnStart: Flow<Boolean> =
        getPreference(CHECK_UPDATE_ON_START).map { it ?: true }

    override suspend fun setCheckUpdateOnStart(value: Boolean) =
        setPreference(CHECK_UPDATE_ON_START, value)

    override val appRightSwipeAction: Flow<ApkActionsOptions> =
        getPreference(APP_RIGHT_SWIPE_ACTION).map {
            SettingsManager.getSwipeActionByPreferenceValue(it) ?: ApkActionsOptions.SAVE
        }

    override suspend fun setRightSwipeAction(value: String) =
        setPreference(APP_RIGHT_SWIPE_ACTION, value)

    override val appLeftSwipeAction: Flow<ApkActionsOptions> =
        getPreference(APP_LEFT_SWIPE_ACTION).map {
            SettingsManager.getSwipeActionByPreferenceValue(it) ?: ApkActionsOptions.SHARE
        }

    override suspend fun setLeftSwipeAction(value: String) =
        setPreference(APP_LEFT_SWIPE_ACTION, value)

    override val appSwipeActionCustomThreshold: Flow<Boolean> =
        getPreference(APP_SWIPE_ACTION_CUSTOM_THRESHOLD).map {
            it ?: false
        }

    override suspend fun setSwipeActionCustomThreshold(value: Boolean) =
        setPreference(APP_SWIPE_ACTION_CUSTOM_THRESHOLD, value)

    override val appSwipeActionThresholdMod: Flow<Float> =
        getPreference(APP_SWIPE_ACTION_THRESHOLD_MODIFIER).map { it ?: 32f }

    override suspend fun setSwipeActionThresholdMod(value: Float) =
        setPreference(APP_SWIPE_ACTION_THRESHOLD_MODIFIER, value)

    override val autoBackupService: Flow<Boolean> =
        getPreference(AUTO_BACKUP_SERVICE).map { it ?: false }

    override suspend fun setAutoBackupService(value: Boolean) =
        setPreference(AUTO_BACKUP_SERVICE, value)

    override val useMaterialYou: Flow<Boolean> = getPreference(MATERIAL_YOU).map { it ?: true }
    override suspend fun setUseMaterialYou(value: Boolean) = setPreference(MATERIAL_YOU, value)

    override val nightMode: Flow<Int> =
        getPreference(NIGHT_MODE).map { it?.toInt() ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM }

    override suspend fun setNightMode(value: Int) = setPreference(NIGHT_MODE, value.toString())
}