package com.wlaxid.scriptflow.editor

import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.amrdeveloper.codeview.CodeView
import java.util.LinkedHashMap
import java.util.regex.Pattern

class EditorController(
    private val codeView: CodeView
) {

    fun init() {
        setupView()
        setupPairs()
        setupSyntax()
    }

    private fun setupView() {

        codeView.setTabLength(4)
        codeView.setEnableAutoIndentation(true)

        codeView.setEnableLineNumber(true)
        codeView.setLineNumberTextSize(50f)
        codeView.setLineNumberTextColor(Color.LTGRAY)

        codeView.setEnableHighlightCurrentLine(true)
        codeView.setHighlightCurrentLineColor(Color.DKGRAY)

        codeView.enablePairComplete(true)
        codeView.enablePairCompleteCenterCursor(true)

        codeView.setTextColor("#D4D4D4".toColorInt())
    }

    fun setText(text: String) {
        codeView.setText(text)
        codeView.reHighlightSyntax()
    }

    fun getText(): String {
        return codeView.text.toString()
    }

    private fun setupPairs() {
        codeView.setPairCompleteMap(
            hashMapOf(
                '{' to '}',
                '[' to ']',
                '(' to ')',
                '<' to '>',
                '"' to '"',
                '\'' to '\''
            )
        )
    }

    private fun setupSyntax() {

        val syntax = LinkedHashMap<Pattern, Int>()

        // """..."""
        syntax[Pattern.compile("(?s)\"\"\".*?\"\"\"")] =
            "#CE9178".toColorInt()

        // '''...'''
        syntax[Pattern.compile("(?s)'''[\\s\\S]*?'''")] =
            "#CE9178".toColorInt()

        // "..."
        syntax[Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"")] =
            "#CE9178".toColorInt()

        // '...'
        syntax[Pattern.compile("'(?:\\\\.|[^'\\\\])*'")] =
            "#CE9178".toColorInt()

        // ключевые слова
        syntax[Pattern.compile(
            "\\b(False|None|True|and|as|assert|break|class|continue|def|elif|else|" +
                    "except|finally|for|from|global|if|import|in|is|lambda|nonlocal|not|or|" +
                    "pass|raise|return|try|while|with|yield)\\b"
        )] = "#569CD6".toColorInt()

        // комментарии

        syntax[Pattern.compile("(?m)#.*$")] =
            "#6A9955".toColorInt()

        codeView.setSyntaxPatternsMap(syntax)
        codeView.reHighlightSyntax()
    }
}