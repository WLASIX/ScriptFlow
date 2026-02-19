package com.wlaxid.scriptflow

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.widget.addTextChangedListener
import androidx.documentfile.provider.DocumentFile
import androidx.drawerlayout.widget.DrawerLayout
import com.amrdeveloper.codeview.CodeView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.wlaxid.scriptflow.editor.EditorController
import com.wlaxid.scriptflow.editor.EditorState
import androidx.core.graphics.toColorInt

class MainActivity : AppCompatActivity() {

    private val editorState = EditorState()
    private lateinit var editorController: EditorController

    private lateinit var codeView: CodeView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var txtFileName: TextView
    private lateinit var btnOptions: ImageView
    private lateinit var itemOpen: LinearLayout
    private lateinit var itemSave: LinearLayout
    private lateinit var fabExecute: FloatingActionButton

    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        bindViews()
        setupEditor()
        setupListeners()

        updateTitle()
    }

    private fun bindViews() {
        codeView = findViewById(R.id.codeView)
        drawerLayout = findViewById(R.id.drawerLayout)
        txtFileName = findViewById(R.id.txtFileName)
        btnOptions = findViewById(R.id.btnOptions)
        itemOpen = findViewById(R.id.itemOpen)
        itemSave = findViewById(R.id.itemSave)
        fabExecute = findViewById(R.id.fabExecute)
    }

    private fun setupEditor() {
        editorController = EditorController(codeView, editorState)
        editorController.init()

        editorController.setText("print(\"Hello World\")")
    }

    private fun setupListeners() {

        codeView.addTextChangedListener {
            if (!editorState.isDirty) {
                editorState.onTextChanged()
                updateTitle()
            }
        }

        fabExecute.setOnClickListener { toggleRunState() }

        itemOpen.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            openFileLauncher.launch(arrayOf("text/*"))
        }

        itemSave.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            saveFileLauncher.launch(editorState.currentFileName)
        }

        btnOptions.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerOpened(drawerView: View) {
                codeView.isEnabled = false
                codeView.clearFocus()
                hideKeyboard()
            }

            override fun onDrawerClosed(drawerView: View) {
                codeView.isEnabled = true
                codeView.requestFocus()
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })
    }

    private fun updateTitle() {
        txtFileName.text = editorState.displayName()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(codeView.windowToken, 0)
    }

    // ================= FILE =================

    private fun getFileName(uri: Uri): String {
        return DocumentFile.fromSingleUri(this, uri)?.name ?: "script.py"
    }

    private fun writeFile(uri: Uri) {
        contentResolver.openOutputStream(uri, "wt")?.use {
            it.write(editorController.getText().toByteArray())
        }
    }

    private val openFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@registerForActivityResult

            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            val text = contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() } ?: return@registerForActivityResult

            val name = getFileName(uri)

            editorController.setText(text)
            editorState.onFileOpened(uri, name)
            updateTitle()
        }

    private val saveFileLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument("text/x-python")
        ) { uri ->
            uri ?: return@registerForActivityResult

            writeFile(uri)

            val name = getFileName(uri)
            editorState.onFileSaved(uri, name)
            updateTitle()
        }

    // ================= RUN =================

    private fun toggleRunState() {
        isRunning = !isRunning

        if (isRunning) {
            fabExecute.setImageResource(R.drawable.ic_stop)
            fabExecute.backgroundTintList =
                ColorStateList.valueOf("#E53935".toColorInt())
        } else {
            fabExecute.setImageResource(R.drawable.ic_start)
            fabExecute.backgroundTintList =
                ColorStateList.valueOf("#2ECC71".toColorInt())
        }
    }
}
