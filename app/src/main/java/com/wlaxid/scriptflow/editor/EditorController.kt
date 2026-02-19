package com.wlaxid.scriptflow.editor

import android.graphics.Color
import com.amrdeveloper.codeview.CodeView
import androidx.core.graphics.toColorInt
import java.util.regex.Pattern

class EditorController(
    private val codeView: CodeView,
    private val editorState: EditorState

) {

    fun init() {
        setupView()
        setupPairs()
        setupSyntax()

    }

    private fun setupView() {
        codeView.setTabLength(4)

        codeView.setEnableLineNumber(true)
        codeView.setLineNumberTextSize(50f)
        codeView.setLineNumberTextColor(Color.WHITE)

        codeView.setEnableHighlightCurrentLine(true)
        codeView.setHighlightCurrentLineColor(Color.GRAY)

        codeView.enablePairComplete(true)
        codeView.enablePairCompleteCenterCursor(true)


    }

    fun setText(text: String) {
        codeView.setText(text)
    }

    fun getText(): String {
        return codeView.text.toString()
    }

    private fun setupPairs() {
        val pairs = hashMapOf(
            '{' to '}',
            '[' to ']',
            '(' to ')',
            '<' to '>',
            '"' to '"',
            '\'' to '\''
        )
        codeView.setPairCompleteMap(pairs)
    }

    private fun setupSyntax() {

        val pythonKeywords = listOf(
            "False","None","True","and","as","assert","break","class","continue","def","del",
            "elif","else","except","finally","for","from","global","if","import","in","is",
            "lambda","nonlocal","not","or","pass","raise","return","try","while","with","yield",
            "async","await"
        )

        val pythonOperators = listOf(
            "==","!=","<=",">=","=",
            "\\+","-","\\*","/","%",
            "\\^","\\|","&","~",
            "<<",">>"
        )

        val keywordPattern        = ("\\b(" + pythonKeywords.joinToString("|") + ")\\b").toRegex()
        val operatorPattern       = ("(" + pythonOperators.joinToString("|") + ")").toRegex()
        val multiStringPattern    = "(\"\"\"[\\s\\S]*?\"\"\"|'{3}[\\s\\S]*?'{3})".toRegex()
        val stringPattern         = "(?<!\\\\)[fF]?[rR]?(?:\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*')".toRegex()
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


}
