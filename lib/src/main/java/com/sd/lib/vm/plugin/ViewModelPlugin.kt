package com.sd.lib.vm.plugin

import android.os.Looper
import androidx.lifecycle.viewModelScope
import com.sd.lib.vm.PluginViewModel

abstract class ViewModelPlugin : PluginViewModel.Plugin {
    private var _vm: PluginViewModel<*>? = null

    protected val vm get() = checkNotNull(_vm) { "Plugin has not been initialized." }

    protected val viewModelScope get() = vm.viewModelScope
    protected val isDestroyed get() = vm.isDestroyed
    protected val isVMActive get() = vm.isVMActive
    protected val isActiveFlow get() = vm.isActiveFlow
    protected val singleDispatcher get() = vm.singleDispatcher

    final override fun notifyInit(viewModel: PluginViewModel<*>) {
        checkMainThread()
        if (_vm != null) error("Plugin has been initialized.")
        _vm = viewModel
        onInit()
    }

    final override fun notifyDestroy() {
        checkMainThread()
        onDestroy()
    }

    /**
     * 初始化(MainThread)
     */
    protected open fun onInit() {}

    /**
     * 销毁(MainThread)
     */
    protected open fun onDestroy() {}
}

private fun checkMainThread() {
    check(Looper.myLooper() === Looper.getMainLooper()) {
        "Expected main thread but was " + Thread.currentThread().name
    }
}