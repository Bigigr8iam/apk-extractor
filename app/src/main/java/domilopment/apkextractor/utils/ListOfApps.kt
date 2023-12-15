package domilopment.apkextractor.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.preference.PreferenceManager
import domilopment.apkextractor.data.ApplicationModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class ListOfApps private constructor(context: Context) {
    private val packageManager = context.packageManager
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val _apps: NonComparingMutableStateFlow<Triple<MutableList<ApplicationModel>, MutableList<ApplicationModel>, MutableList<ApplicationModel>>> =
        NonComparingMutableStateFlow(
            Triple(mutableListOf(), mutableListOf(), mutableListOf())
        )

    val apps: Flow<Triple<List<ApplicationModel>, List<ApplicationModel>, List<ApplicationModel>>> =
        _apps.asStateFlow()

    // initialize APK list
    init {
        updateData()
    }

    /**
     * Update Installed APK lists
     */
    fun updateData() {
        // Ensure all list are Empty!
        val newUpdatedSystemApps = mutableListOf<ApplicationModel>()
        val newSystemApps = mutableListOf<ApplicationModel>()
        val newUserApps = mutableListOf<ApplicationModel>()
        // Fill each list with its specific type
        val applicationsInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(
                    PackageManager.GET_META_DATA.toLong()
                )
            )
        } else {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        }

        val favorites =
            sharedPreferences.getStringSet(Constants.PREFERENCE_KEY_FAVORITES, setOf()) ?: setOf()
        applicationsInfo.forEach { packageInfo: ApplicationInfo ->
            ApplicationModel(
                packageManager = packageManager,
                appPackageName = packageInfo.packageName,
                isFavorite = packageInfo.packageName in favorites
            ).also {
                when {
                    (it.appFlags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == ApplicationInfo.FLAG_UPDATED_SYSTEM_APP -> newUpdatedSystemApps.add(
                        it
                    )

                    (it.appFlags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM -> newSystemApps.add(
                        it
                    )

                    else -> newUserApps.add(it)
                }
            }
        }

        _apps.value = Triple(newUpdatedSystemApps, newSystemApps, newUserApps)
    }

    suspend fun add(app: ApplicationModel) {
        val apps = _apps.value.copy()
        val updatedSysApps = apps.first.toMutableList()
        val sysApps = apps.second.toMutableList()
        val userApps = apps.third.toMutableList()

        when (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) {
            (app.appFlags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) -> {
                if (updatedSysApps.find { it.appPackageName == app.appPackageName } == null) updatedSysApps.add(
                    app
                )
                sysApps.removeIf { it.appPackageName == app.appPackageName }
            }

            else -> if (userApps.find { it.appPackageName == app.appPackageName } == null) userApps.add(
                app
            )
        }

        _apps.value = Triple(updatedSysApps, sysApps, userApps)
    }

    suspend fun remove(app: ApplicationModel) {
        val apps = _apps.value.copy()
        val updatedSysApps = apps.first.toMutableList()
        val sysApps = apps.second.toMutableList()
        val userApps = apps.third.toMutableList()

        if (updatedSysApps.removeIf { it.appPackageName == app.appPackageName }) sysApps.add(app)
        else userApps.removeIf { it.appPackageName == app.appPackageName }

        _apps.value = Triple(updatedSysApps, sysApps, userApps)
    }

    companion object {
        private lateinit var INSTANCE: ListOfApps

        fun getApplications(context: Context): ListOfApps {
            synchronized(ListOfApps::class.java) {
                if (!::INSTANCE.isInitialized) {
                    INSTANCE = ListOfApps(context.applicationContext)
                }
            }
            return INSTANCE
        }
    }
}
