package com.wlaxid.scriptflow.ui.console

import com.wlaxid.scriptflow.runtime.RunState

class ConsoleOutputPresenter(
    private val consoleController: ConsoleController
) {

    fun onStateChanged(state: RunState) {
        when (state) {
            RunState.Running -> {
                consoleController.clear()
                consoleController.appendMessage(
                    "=== Running ===\n",
                    ConsoleMessageType.SYSTEM
                )
            }

            RunState.Finished -> {
                consoleController.appendMessage(
                    "=== Finished ===",
                    ConsoleMessageType.SYSTEM
                )
            }

            RunState.Error -> {
                consoleController.appendMessage(
                    "=== Finished with errors ===",
                    ConsoleMessageType.SYSTEM
                )
            }

            RunState.Cancelled -> {
                consoleController.appendMessage(
                    "=== Cancelled ===",
                    ConsoleMessageType.SYSTEM
                )
            }
        }
    }

    fun onOutput(text: String) {
        consoleController.appendMessage(
            text,
            ConsoleMessageType.OUTPUT
        )
    }

    fun onError(text: String) {
        consoleController.appendMessage(
            text,
            ConsoleMessageType.ERROR
        )
    }
}
