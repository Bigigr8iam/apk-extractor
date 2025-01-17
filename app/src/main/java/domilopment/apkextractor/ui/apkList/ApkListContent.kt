package domilopment.apkextractor.ui.apkList

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import domilopment.apkextractor.data.apkList.PackageArchiveModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkListContent(
    apkList: List<PackageArchiveModel>,
    searchString: String?,
    refreshing: Boolean,
    isPullToRefresh: Boolean,
    onRefresh: () -> Unit,
    onClick: (PackageArchiveModel) -> Unit,
    deletedDocumentFound: (PackageArchiveModel) -> Unit
) {
    val state = rememberPullToRefreshState(enabled = { isPullToRefresh })
    if (state.isRefreshing) {
        LaunchedEffect(true) {
            if (!refreshing) onRefresh()
        }
    }
    LaunchedEffect(refreshing) {
        if (refreshing && !state.isRefreshing) state.startRefresh()
        else if (!refreshing && state.isRefreshing) state.endRefresh()
    }

    Box(Modifier.nestedScroll(state.nestedScrollConnection)) {
        ApkList(
            apkList = apkList,
            searchString = searchString,
            onClick = onClick,
            deletedDocumentFound = deletedDocumentFound
        )

        PullToRefreshContainer(state = state, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Preview
@Composable
private fun ApkListScreenPreview() {
    val apks = remember {
        mutableStateListOf(
            PackageArchiveModel(
                fileUri = Uri.parse("test"),
                fileName = "Test.apk",
                fileLastModified = 0L,
                fileSizeLong = 1024L,
                appName = "Test",
                appPackageName = "com.example.test",
                appVersionCode = 1L,
                appVersionName = "1.0",
            ), PackageArchiveModel(
                fileUri = Uri.parse("test2"),
                fileName = "Test2.apk",
                fileLastModified = 0L,
                fileSizeLong = 1024L,
                appName = "Test 2",
                appPackageName = "com.example.test2",
                appVersionCode = 1L,
                appVersionName = "1.0",
            ), PackageArchiveModel(
                fileUri = Uri.parse("test (2)"),
                fileName = "Test (2).apk",
                fileLastModified = 0L,
                fileSizeLong = 1024L,
                appName = "Test",
                appPackageName = "com.example.test",
                appVersionCode = 2L,
                appVersionName = "1.0.1",
            )
        )
    }
    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val apkToAdd = PackageArchiveModel(
        fileUri = Uri.parse("test (3)"),
        fileName = "Test (3).apk",
        fileLastModified = 0L,
        fileSizeLong = 1024L,
        appName = "Test",
        appPackageName = "com.example.test",
        appVersionCode = 3L,
        appVersionName = "1.1.0",
    )

    MaterialTheme {
        Column {
            ApkListContent(apkList = apks,
                searchString = "",
                refreshing = refreshing,
                isPullToRefresh = true,
                onRefresh = {
                    refreshScope.launch {
                        refreshing = true
                        delay(1500)
                        if (!apks.contains(apkToAdd)) apks.add(apkToAdd)
                        refreshing = false
                    }
                },
                onClick = { _ -> },
                deletedDocumentFound = { _ -> })
        }
    }
}