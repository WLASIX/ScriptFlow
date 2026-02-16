package com.wlaxid.scriptflow

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.amrdeveloper.codeview.CodeView
import androidx.core.graphics.toColorInt
import androidx.core.view.GravityCompat
import androidx.documentfile.provider.DocumentFile
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private var isRunning = false
    private lateinit var fabExecute: FloatingActionButton
    private lateinit var itemOpen: LinearLayout
    private lateinit var itemSave: LinearLayout
    private lateinit var codeView: CodeView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnOptions: ImageView
    private var currentFileUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        codeView = findViewById(R.id.codeView)
        itemOpen = findViewById(R.id.itemOpen)
        itemSave = findViewById(R.id.itemSave)
        drawerLayout = findViewById(R.id.drawerLayout)
        btnOptions = findViewById(R.id.btnOptions)
        fabExecute = findViewById(R.id.fabExecute)

        setPairs()
        setSyntax()

        codeView.setText("print(\"Hello World\")")

        // Пока не обрабатывается табуляция
        codeView.setTabLength(4);

        // Номер строки
        codeView.setEnableLineNumber(true);
        codeView.setLineNumberTextSize(50f);
        codeView.setLineNumberTextColor(Color.WHITE)

        // Текущая линия
        codeView.setEnableHighlightCurrentLine(true)
        codeView.setHighlightCurrentLineColor(Color.GRAY);

        // Парные кавычки, скобки
        codeView.enablePairComplete(true);
        codeView.enablePairCompleteCenterCursor(true);

        // Listeners
        fabExecute.setOnClickListener {
            toggleRunState()
        }
        itemOpen.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            openFileLauncher.launch(arrayOf("text/*"))
        }
        itemSave.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)

            val suggestedName =
                currentFileUri?.let { getFileName(it) } ?: "script.py"

            saveFileLauncher.launch(suggestedName)
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
                // убрать фокус
                codeView.clearFocus()

                // скрыть клавиатуру
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(codeView.windowToken, 0)

            }

            override fun onDrawerClosed(drawerView: View) {
                codeView.isEnabled = true
                codeView.requestFocus()
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })

    }

// ================= FILE UTILS =================

    private fun getFileName(uri: Uri): String {
        return DocumentFile.fromSingleUri(this, uri)?.name ?: "script.py"
    }

    private fun writeFile(uri: Uri) {
        contentResolver.openOutputStream(uri, "wt")?.use { output ->
            output.write(codeView.text.toString().toByteArray())
        }
    }

// ================= PAIRS =================

    private fun setPairs() {
        val pairCompleteMap = hashMapOf(
            '{' to '}',
            '[' to ']',
            '(' to ')',
            '<' to '>',
            '"' to '"',
            '\'' to '\''
        )
        codeView.setPairCompleteMap(pairCompleteMap)
    }

// ================= SYNTAX =================

    private fun setSyntax() {

        val pythonKeywords = listOf(
            "False","None","True","and","as","assert","break","class","continue","def","del",
            "elif","else","except","finally","for","from","global","if","import","in","is",
            "lambda","nonlocal","not","or","pass","raise","return","try","while","with","yield",
            "async","await"
        )

        val pythonOperators = listOf(
            "==","!=","<=",">=","=","\\+","-","\\*","/","%","\\^","\\|","&","~","<<",">>"
        )

        val keywordPattern        = ("\\b(" + pythonKeywords.joinToString("|") + ")\\b").toRegex()
        val operatorPattern       = ("(" + pythonOperators.joinToString("|") + ")").toRegex()
        val multiStringPattern    = "(\"\"\"[\\s\\S]*?\"\"\"|'{3}[\\s\\S]*?'{3})".toRegex()
        val stringPattern         = "(?<!\\\\)(?:[fF]?[rR]?(?:\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'))".toRegex()
        val commentPattern        = "#.*".toRegex()
        val numberPattern         = "\\b(?:0b[01_]+|0x[0-9A-Fa-f_]+|\\d[\\d_]*(?:\\.\\d[\\d_]*)?)\\b".toRegex()
        val funcNamePattern       = "(?<=def\\s)\\w+".toRegex()
        val classNamePattern      = "(?<=class\\s)\\w+".toRegex()
        val decoratorPattern      = "@\\w+".toRegex()
        val typeAnnotationPattern = ":\\s*(\\w+)".toRegex()

        val syntaxPatterns = mutableMapOf<Pattern, Int>().apply {
            this[keywordPattern.toPattern()]        = "#569CD6".toColorInt()
            this[operatorPattern.toPattern()]       = "#D4D4D4".toColorInt()
            this[multiStringPattern.toPattern()]    = "#CE9178".toColorInt()
            this[stringPattern.toPattern()]         = "#CE9178".toColorInt()
            this[commentPattern.toPattern()]        = "#6A9955".toColorInt()
            this[numberPattern.toPattern()]         = "#B5CEA8".toColorInt()
            this[funcNamePattern.toPattern()]       = "#DCDCAA".toColorInt()
            this[classNamePattern.toPattern()]      = "#4EC9B0".toColorInt()
            this[decoratorPattern.toPattern()]      = "#C586C0".toColorInt()
            this[typeAnnotationPattern.toPattern()] = "#9CDCFE".toColorInt()
        }

        codeView.setSyntaxPatternsMap(syntaxPatterns)
        codeView.reHighlightSyntax()
    }

// ================= RUN / STOP =================

    private fun toggleRunState() {
        isRunning = !isRunning

        if (isRunning) {
            fabExecute.setImageResource(R.drawable.ic_stop)
            fabExecute.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor("#E53935")) // красный
        } else {
            fabExecute.setImageResource(R.drawable.ic_start)
            fabExecute.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor("#2ECC71")) // зелёный
        }
    }

// ================= OPEN =================

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
                ?.use { it.readText() }
                ?: return@registerForActivityResult

            codeView.setText(text)
            currentFileUri = uri
        }

// ================= SAVE =================

    private val saveFileLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument("text/x-python")
        ) { uri ->
            uri ?: return@registerForActivityResult
            writeFile(uri)
            currentFileUri = uri
        }

    private fun saveCurrentFile() {
        if (currentFileUri != null) {
            writeFile(currentFileUri!!)
        } else {
            saveFileLauncher.launch("script.py")
        }
    }


}