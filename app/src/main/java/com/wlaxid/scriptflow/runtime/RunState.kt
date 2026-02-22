package com.wlaxid.scriptflow.runtime

sealed class RunState {
    object Running : RunState()
    object Stopped : RunState()
}