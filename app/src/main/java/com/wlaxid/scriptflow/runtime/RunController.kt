package com.wlaxid.scriptflow.runtime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process

class RunController(
    private val context: Context,
    private val onStateChanged: (RunState) -> Unit,
    private val onOutput: (String) -> Unit,
    private val onError: (String) -> Unit
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var childPid: Int = -1
    private var isRunning = false
    private var errorOccurred = false
    private var cancelled = false
    private var currentRunId = 0
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val runId = intent.getIntExtra(PythonRunnerService.EXTRA_RUN_ID, -1)
            if (runId != -1 && runId != currentRunId) {
                return
            }

            when (action) {
                PythonRunnerService.BROADCAST_STARTED -> {
                    childPid = intent.getIntExtra(PythonRunnerService.EXTRA_PID, -1)
                    isRunning = true
                    errorOccurred = false
                    cancelled = false
                    mainHandler.post { onStateChanged(RunState.Running) }
                }

                PythonRunnerService.BROADCAST_OUTPUT -> {
                    val text = intent.getStringExtra(PythonRunnerService.EXTRA_TEXT) ?: ""
                    mainHandler.post { onOutput(text) }
                }

                PythonRunnerService.BROADCAST_ERROR -> {
                    val text = intent.getStringExtra(PythonRunnerService.EXTRA_TEXT) ?: ""
                    errorOccurred = true
                    mainHandler.post { onError(text) }
                }

                PythonRunnerService.BROADCAST_FINISHED -> {
                    isRunning = false
                    childPid = -1

                    val finalState =
                        when {
                            cancelled -> RunState.Cancelled
                            errorOccurred -> RunState.Error
                            else -> RunState.Finished
                        }

                    mainHandler.post { onStateChanged(finalState) }
                }
            }
        }
    }
    init {
        val filter = IntentFilter().apply {
            addAction(PythonRunnerService.BROADCAST_STARTED)
            addAction(PythonRunnerService.BROADCAST_OUTPUT)
            addAction(PythonRunnerService.BROADCAST_ERROR)
            addAction(PythonRunnerService.BROADCAST_FINISHED)
        }
        context.applicationContext.registerReceiver(
            receiver,
            filter,
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    fun execute(code: String) {
        if (isRunning) return
        currentRunId++
        val runId = currentRunId

        val intent = Intent(context, PythonRunnerService::class.java).apply {
            action = PythonRunnerService.ACTION_EXECUTE
            putExtra(PythonRunnerService.EXTRA_CODE, code)
            putExtra(PythonRunnerService.EXTRA_RUN_ID, runId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stop(forceKillImmediately: Boolean = false, killDelayMs: Long = 10) {
        cancelled = true

        val stopIntent = Intent(context, PythonRunnerService::class.java).apply {
            action = PythonRunnerService.ACTION_STOP
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(stopIntent)
        } else {
            context.startService(stopIntent)
        }

        if (forceKillImmediately) {
            killChildProcess()
        } else {
            // даю короткую задержку для братского (нет) завершения, а затем насильно убиваю
            mainHandler.postDelayed({
                if (isRunning && childPid > 0) {
                    killChildProcess()
                }
            }, killDelayMs)
        }
    }

    private fun killChildProcess() {
        if (childPid > 0) {
            try {
                Process.killProcess(childPid)
            } catch (_: Exception) { }

            childPid = -1
            isRunning = false
            cancelled = true

            mainHandler.post {
                onStateChanged(RunState.Cancelled)
            }
        }
    }

    fun currentState(): RunState =
        if (isRunning) RunState.Running
        else RunState.Finished

    fun destroy() {
        try {
            context.applicationContext.unregisterReceiver(receiver)
        } catch (_: Exception) { }
    }
}