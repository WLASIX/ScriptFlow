package com.wlaxid.scriptflow.runtime

sealed class RunState {
    object Running : RunState()
    object Finished : RunState()
    object Error : RunState()
    object Cancelled : RunState()
}