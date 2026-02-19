package com.wlaxid.scriptflow

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.addTextChangedListener
import androidx.drawerlayout.widget.DrawerLayout
import com.amrdeveloper.codeview.CodeView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.wlaxid.scriptflow.editor.EditorController
import com.wlaxid.scriptflow.editor.EditorState
import com.wlaxid.scriptflow.editor.FileController
import com.wlaxid.scriptflow.runtime.RunController
import com.wlaxid.scriptflow.runtime.RunState
import com.wlaxid.scriptflow.ui.toolbar.ToolbarController

class MainActivity : AppCompatActivity() {

    private val editorState = EditorState()
    private lateinit var editorController: EditorController
    private lateinit var fileController: FileController
    private lateinit var codeView: CodeView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var itemOpen: LinearLayout
    private lateinit var itemSave: LinearLayout
    private lateinit var fabExecute: FloatingActionButton
    private lateinit var toolbarController: ToolbarController
    private lateinit var runController: RunController


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        bindViews()
        runController = RunController()
        renderRunState(runController.currentState())
        setupEditor()
        setupListeners()
        setupFileController()
        setupToolbar()
        toolbarController.setTitle(editorState.displayName())

    }

    private fun bindViews() {
        codeView = findViewById(R.id.codeView)
        drawerLayout = findViewById(R.id.drawerLayout)
        itemOpen = findViewById(R.id.itemOpen)
        itemSave = findViewById(R.id.itemSave)
        fabExecute = findViewById(R.id.fabExecute)
    }


    private fun setupEditor() {
        editorController = EditorController(codeView)
        editorController.init()

        editorController.setText("print(\"Hello World\")")
    }

    private fun setupFileController() {
        fileController = FileController(
            activity = this,
            editorController = editorController,
            onFileOpened = { uri, name ->
                editorState.onFileOpened(uri, name)
                toolbarController.setTitle(editorState.displayName())
            },
            onFileSaved = { uri, name ->
                editorState.onFileSaved(uri, name)
                toolbarController.setTitle(editorState.displayName())
            }
        )
    }


    private fun setupListeners() {

        codeView.addTextChangedListener {
            if (!editorState.isDirty) {
                editorState.onTextChanged()
                toolbarController.setTitle(editorState.displayName())
            }
        }

        fabExecute.setOnClickListener {
            val state = runController.toggle()
            renderRunState(state)
        }


        itemOpen.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            fileController.open()
        }

        itemSave.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            fileController.save(editorState.currentFileName)
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
    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(codeView.windowToken, 0)
    }

    private fun setupToolbar() {
        val toolbarRoot = findViewById<View>(R.id.topToolbar)

        toolbarController = ToolbarController(
            root = toolbarRoot,
            drawerLayout = drawerLayout
            // остальное пока заглушки
        )
    }

    private fun renderRunState(state: RunState) {
        fabExecute.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES

        when (state) {
            RunState.Running -> {
                fabExecute.setImageResource(R.drawable.ic_stop)
                fabExecute.backgroundTintList =
                    ColorStateList.valueOf("#E53935".toColorInt())

                fabExecute.contentDescription =
                    getString(R.string.action_stop)

                ViewCompat.setTooltipText(
                    fabExecute,
                    getString(R.string.action_stop)
                )
            }

            RunState.Stopped -> {
                fabExecute.setImageResource(R.drawable.ic_start)
                fabExecute.backgroundTintList =
                    ColorStateList.valueOf("#2ECC71".toColorInt())

                fabExecute.contentDescription =
                    getString(R.string.action_run)

                ViewCompat.setTooltipText(
                    fabExecute,
                    getString(R.string.action_run)
                )
            }
        }
    }


}
