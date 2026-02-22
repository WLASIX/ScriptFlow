package com.wlaxid.scriptflow.runtime

import android.os.Handler
import android.os.Looper
import com.chaquo.python.Python

class RunController(
    private val onStateChanged: (RunState) -> Unit,
    private val onOutput: (String) -> Unit,
    private val onError: (String) -> Unit
) {

    @Volatile
    private var isRunning = false

    private var thread: Thread? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun execute(code: String): RunState {
        if (isRunning) return currentState()
        isRunning = true

        mainHandler.post {
            onStateChanged(RunState.Running)
        }

        thread = Thread {
            try {
                val py = Python.getInstance()
                val builtins = py.getModule("builtins")

                val globals = builtins.callAttr("dict")
                globals.callAttr("__setitem__", "__builtins__", builtins)
                globals.callAttr("__setitem__", "__user_code__", code)

                val wrapper =
                    """
                    import sys, io, traceback
                    
                    buf = io.StringIO()
                    error = False
                    
                    _old_out = sys.stdout
                    _old_err = sys.stderr
                    sys.stdout = buf
                    sys.stderr = buf
                    
                    try:
                        exec(__user_code__, globals(), globals())
                    except Exception:
                        error = True
                        traceback.print_exc()
                    finally:
                        sys.stdout = _old_out
                        sys.stderr = _old_err
                    
                    __output__ = buf.getvalue()
                    __error__ = error
                    """.trimIndent()

                builtins.callAttr("exec", wrapper, globals, globals)

                val output = globals.callAttr("__getitem__", "__output__")
                    ?.toString()
                    ?: ""

                val isError = globals.callAttr("__getitem__", "__error__")
                    ?.toJava(Boolean::class.java)
                    ?: false

                if (output.isNotBlank()) {
                    mainHandler.post {
                        if (isError) {
                            onError(output)
                        } else {
                            onOutput(output)
                        }
                    }
                }

            } catch (e: Exception) {
                mainHandler.post {
                    onError(e.stackTraceToString())
                }
            } finally {
                isRunning = false
                mainHandler.post {
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

        mainHandler.post {
            onStateChanged(RunState.Stopped)
        }

        return currentState()
    }

    fun currentState(): RunState =
        if (isRunning) RunState.Running else RunState.Stopped
}