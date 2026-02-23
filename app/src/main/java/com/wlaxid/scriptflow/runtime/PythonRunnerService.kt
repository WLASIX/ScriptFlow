package com.wlaxid.scriptflow.runtime

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import com.chaquo.python.Python
import com.chaquo.python.PyObject
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.wlaxid.scriptflow.R

class PythonRunnerService : Service() {

    companion object {
        const val CHANNEL_ID = "scriptflow_runner"

        const val ACTION_EXECUTE = "com.wlaxid.scriptflow.python.ACTION_EXECUTE"
        const val ACTION_STOP = "com.wlaxid.scriptflow.python.ACTION_STOP"
        const val EXTRA_CODE = "extra_code"

        const val BROADCAST_STARTED = "com.wlaxid.scriptflow.python.BROADCAST_STARTED"
        const val BROADCAST_OUTPUT = "com.wlaxid.scriptflow.python.BROADCAST_OUTPUT"
        const val BROADCAST_ERROR = "com.wlaxid.scriptflow.python.BROADCAST_ERROR"
        const val BROADCAST_FINISHED = "com.wlaxid.scriptflow.python.BROADCAST_FINISHED"

        const val EXTRA_PID = "extra_pid"
        const val EXTRA_TEXT = "extra_text"
    }

    private var py: Python? = null
    private var globals: PyObject? = null
    private var execThread: Thread? = null

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Script execution",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        startForeground(
            1,
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_start)
                .setContentTitle("ScriptFlow")
                .setContentText("Running script")
                .setOngoing(true)
                .build()
        )

        py = Python.getInstance()

        val pid = Process.myPid()
        sendBroadcast(
            Intent(BROADCAST_STARTED)
                .putExtra(EXTRA_PID, pid)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_EXECUTE -> {
                val code = intent.getStringExtra(EXTRA_CODE) ?: ""
                executeUserCode(code)
            }
            ACTION_STOP -> {
                try {
                    globals?.callAttr("__setitem__", "__cancelled__", true)
                } catch (_: Exception) { }
            }
        }
        return START_NOT_STICKY
    }

    private fun executeUserCode(code: String) {
        if (execThread?.isAlive == true) return

        execThread = Thread {
            try {
                val pyInst = py ?: Python.getInstance()
                val builtins = pyInst.getModule("builtins")

                globals = builtins.callAttr("dict")
                globals!!.callAttr("__setitem__", "__builtins__", builtins)
                globals!!.callAttr("__setitem__", "__user_code__", code)
                globals!!.callAttr("__setitem__", "__cancelled__", false)

                val wrapper =
                    """
                    import sys, io, traceback, time
                    
                    out_buf = io.StringIO()
                    err_buf = io.StringIO()
                    error = False
                    
                    _old_out = sys.stdout
                    _old_err = sys.stderr
                    
                    sys.stdout = out_buf
                    sys.stderr = err_buf
                    
                    def _check_cancel(frame, event, arg):
                        if globals().get("__cancelled__"):
                            raise KeyboardInterrupt("cancelled")
                        #time.sleep(0.001)
                        return _check_cancel
                    
                    sys.settrace(_check_cancel)
                    
                    try:
                        exec(__user_code__, globals(), globals())
                    except KeyboardInterrupt:
                        error = True
                        print("Execution cancelled by user", file=sys.stderr)
                    except Exception:
                        error = True
                        traceback.print_exc(file=sys.stderr)
                    finally:
                        sys.settrace(None)
                        sys.stdout = _old_out
                        sys.stderr = _old_err
                    
                    __stdout__ = out_buf.getvalue()
                    __stderr__ = err_buf.getvalue()
                    __error__ = error
                    """.trimIndent()

                builtins.callAttr("exec", wrapper, globals, globals)

                val stdout = globals!!
                    .callAttr("__getitem__", "__stdout__")
                    ?.toString()
                    ?: ""

                val stderr = globals!!
                    .callAttr("__getitem__", "__stderr__")
                    ?.toString()
                    ?: ""

                val isError = globals!!
                    .callAttr("__getitem__", "__error__")
                    ?.toJava(Boolean::class.java)
                    ?: false

                if (stdout.isNotBlank()) {
                    sendBroadcast(
                        Intent(BROADCAST_OUTPUT)
                            .setPackage(packageName)
                            .putExtra(EXTRA_TEXT, stdout)
                    )
                }

                if (stderr.isNotBlank()) {
                    sendBroadcast(
                        Intent(BROADCAST_ERROR)
                            .setPackage(packageName)
                            .putExtra(EXTRA_TEXT, stderr)
                    )
                } else if (isError) {
                    // если ошибка была, но stderr пуст
                    sendBroadcast(
                        Intent(BROADCAST_ERROR)
                            .setPackage(packageName)
                            .putExtra(EXTRA_TEXT, "Script failed (no stderr captured)")
                    )
                }
            } catch (t: Throwable) {
                sendBroadcast(
                    Intent(BROADCAST_ERROR)
                        .setPackage(packageName)
                        .putExtra(EXTRA_TEXT, t.stackTraceToString())
                )
            } finally {
                sendBroadcast(
                    Intent(BROADCAST_FINISHED)
                        .setPackage(packageName)
                )
                stopSelf()
            }
        }.also { thread ->
            thread.uncaughtExceptionHandler =
                Thread.UncaughtExceptionHandler { _, ex ->
                    sendBroadcast(
                        Intent(BROADCAST_ERROR)
                            .setPackage(packageName)
                            .putExtra(EXTRA_TEXT, "UNCAUGHT:\n${ex.stackTraceToString()}")
                    )
                }
            thread.start()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try { globals?.callAttr("__setitem__", "__cancelled__", true) } catch (_: Exception) {}
        super.onDestroy()
    }
}