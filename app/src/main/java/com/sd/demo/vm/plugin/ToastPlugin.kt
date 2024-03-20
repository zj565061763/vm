package com.sd.demo.vm.plugin

import com.sd.lib.vm.plugin.StateVMPlugin
import com.sd.lib.vm.plugin.RealVMPlugin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

interface ToastPlugin : StateVMPlugin<Flow<ToastPlugin.State>> {
    fun showToast(msg: String)

    data class State(
        val msg: String
    )
}

fun ToastPlugin(): ToastPlugin {
    return ToastPluginImpl()
}

private class ToastPluginImpl : RealVMPlugin(), ToastPlugin {
    private val _state = MutableSharedFlow<ToastPlugin.State>()
    override val state: Flow<ToastPlugin.State> = _state.asSharedFlow()

    override fun showToast(msg: String) {
        vmScope.launch {
            _state.emit(ToastPlugin.State(msg = msg))
        }
    }
}