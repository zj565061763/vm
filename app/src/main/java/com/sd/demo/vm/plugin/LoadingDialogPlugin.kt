package com.sd.demo.vm.plugin

import com.sd.lib.vm.plugin.RealVMPlugin
import com.sd.lib.vm.plugin.StateVMPlugin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface LoadingDialogPlugin : StateVMPlugin<StateFlow<LoadingDialogPlugin.State?>> {
    /**
     * 显示加载框
     */
    fun showLoading(msg: String)

    /**
     * 隐藏加载框
     */
    fun hideLoading()

    data class State(
        val msg: String
    )
}

fun LoadingDialogPlugin(): LoadingDialogPlugin {
    return LoadingDialogPluginImpl()
}

private class LoadingDialogPluginImpl : RealVMPlugin(), LoadingDialogPlugin {
    private val _state = MutableStateFlow<LoadingDialogPlugin.State?>(null)
    override val state: StateFlow<LoadingDialogPlugin.State?> = _state.asStateFlow()

    override fun showLoading(msg: String) {
        _state.update { LoadingDialogPlugin.State(msg = msg) }
    }

    override fun hideLoading() {
        _state.update { null }
    }
}



