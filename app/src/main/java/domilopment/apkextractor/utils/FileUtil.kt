package domilopment.apkextractor.utils

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import domilopment.apkextractor.BuildConfig
import domilopment.apkextractor.data.ApplicationModel
import java.io.*

class FileUtil(private val context: Context) {
    companion object {
        const val MIME_TYPE = "application/vnd.android.package-archive"
        const val PREFIX = ".apk"
    }

    /**
     * Copy APK file from Source to Chosen Save Director with Name
     * @param from
     * Source of existing APK from App
     * @param to
     * Destination Folder for APK
     * @param fileName
     * Name for Saved APK File
     * @return
     * True if copy was Successfully else False
     */
    fun copy(
        from: String, to: Uri, fileName: String
    ): Uri? {
        return try {
            val extractedApk: Uri?
            // Create Input Stream from APK source file
            FileInputStream(from).use { input ->
                // Create new APK file in destination folder
                DocumentsContract.createDocument(
                    context.contentResolver, DocumentsContract.buildDocumentUriUsingTree(
                        to, DocumentsContract.getTreeDocumentId(to)
                    ), MIME_TYPE, fileName
                ).let { outputFile ->
                    extractedApk = outputFile
                    // Create Output Stream for target APK file
                    context.contentResolver.openOutputStream(outputFile!!)
                }.use { output ->
                    // Copy from Input to Output Stream
                    input.copyTo(output!!)
                }
            }
            extractedApk
        } catch (fnf_e: FileNotFoundException) {
            fnf_e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Creates a Uri for Provider
     * @param app Application for sharing
     * @return Shareable Uri of Application APK
     */
    fun shareURI(app: ApplicationModel): Uri {
        return FileProvider.getUriForFile(
            context, BuildConfig.APPLICATION_ID + ".provider", File(app.appSourceDirectory).copyTo(
                File(
                    context.cacheDir, SettingsManager(context).appName(app)
                ), true
            )
        )
    }

    /**
     * Takes uri from Document file, and checks if Document exists
     * @param uri Document uri
     * @return true if document exist else false
     */
    fun doesDocumentExist(uri: Uri): Boolean {
        val documentId =
            if (DocumentsContract.isDocumentUri(context, uri)) DocumentsContract.getDocumentId(
                uri
            ) else DocumentsContract.getTreeDocumentId(uri)
        return try {
            context.contentResolver.query(
                DocumentsContract.buildDocumentUriUsingTree(
                    uri, documentId
                ), arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null
            )?.use { cursor -> cursor.count > 0 } ?: false
        } catch (e: Exception) {
            false
        }
    }
}