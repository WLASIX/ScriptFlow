package com.wlaxid.scriptflow.runtime

import android.os.Handler
import android.os.Looper
import com.chaquo.python.Python

class RunController(private val onStateChanged: (RunState) -> Unit) {

    private var isRunning = false
    private var thread: Thread? = null

    fun execute(code: String): RunState {
        if (isRunning) return currentState()
        isRunning = true

        thread = Thread {
            try {
                val python = Python.getInstance()
                val builtins = python.getModule("builtins")

                val globals = builtins.callAttr("dict")
                globals.callAttr("__setitem__", "__builtins__", builtins)

                builtins.callAttr("exec", code, globals, globals)

            } catch (e: Exception) {
                e.printStackTrace() // тут потом выведу в свою консоль
            } finally {
                isRunning = false

                Handler(Looper.getMainLooper()).post {
                    onStateChanged(RunState.Stopped)
                }
            }
        }.also { it.start() }

        return currentState()
    }

    fun stop(): RunState {
        thread?.interrupt()
        thread = null
        isRunning = false
        return currentState()
    }

    fun currentState(): RunState =
        if (isRunning) RunState.Running else RunState.Stopped
}

sealed class RunState {
    object Running : RunState()
    object Stopped : RunState()
}