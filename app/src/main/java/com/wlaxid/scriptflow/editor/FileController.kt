package com.wlaxid.scriptflow.editor

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import com.wlaxid.scriptflow.R

class FileController(
    private val activity: ComponentActivity,
    private val editorController: EditorController,
    private val onFileOpened: (Uri, String) -> Unit,
    private val onFileSaved: (Uri, String) -> Unit
) {
    private val openLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            uri ?: return@registerForActivityResult

            takePermissions(uri)

            val text = activity.contentResolver
                .openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: return@registerForActivityResult

            val name = getFileName(uri)

            editorController.setText(text)
            onFileOpened(uri, name)
        }

    private val saveLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(
            ActivityResultContracts.CreateDocument("text/x-python")
        ) { uri: Uri? ->
            uri ?: return@registerForActivityResult

            writeFile(uri)

            val name = getFileName(uri)
            onFileSaved(uri, name)
        }

    fun open() {
        openLauncher.launch(arrayOf("text/*"))
    }

    fun save(fileName: String) {
        saveLauncher.launch(fileName)
    }

    fun quickSave(currentUri: Uri?, suggestedName: String) {
        if (currentUri != null) {
            writeFile(currentUri)
            onFileSaved(currentUri, getFileName(currentUri))
        } else {
            save(suggestedName)
        }
    }

    private fun writeFile(uri: Uri) {
        activity.contentResolver.openOutputStream(uri, "wt")?.use {
            it.write(editorController.getText().toByteArray())
        }
    }

    private fun getFileName(uri: Uri): String {
        return DocumentFile.fromSingleUri(activity, uri)?.name
            ?: activity.getString(R.string.default_filename)
    }

    private fun takePermissions(uri: Uri) {
        activity.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }
}
