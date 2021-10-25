package com.example.wowwaw.fs

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream

fun getFileNameFromUri(uri: Uri, contentResolver: ContentResolver): String? {
    return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        return cursor.getString(index)
    }
}

fun saveFileIntoExternalStorageByUri(
    uri: Uri,
    externalFolder: File,
    contentResolver: ContentResolver
): File? {
    val inputStream: InputStream = contentResolver.openInputStream(uri) ?: return null
    val fileName: String = getFileNameFromUri(uri, contentResolver) ?: return null
    val fileCopy = File(externalFolder, fileName)
    try {
        fileCopy.outputStream().use { inputStream.copyTo(it) }
    } catch (ex: IOException) {
        Timber.e(ex, "Cant copy file to external storage $ex")
        return null
    } finally {
        inputStream.close()
    }
    return fileCopy
}