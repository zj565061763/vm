package com.sd.lib.vm.plugin

import android.os.Looper
import androidx.lifecycle.viewModelScope
import com.sd.lib.vm.FViewModel
import com.sd.lib.vm.PluginViewModel

abstract class ViewModelPlugin : PluginViewModel.Plugin {
    private var _vm: PluginViewModel<*>? = null

    protected val vm get() = checkNotNull(_vm) { "Plugin has not been initialized." }

    /** [FViewModel.viewModelScope] */
    protected val viewModelScope get() = vm.viewModelScope

    /** [FViewModel.isDestroyed] */
    protected val isDestroyed get() = vm.isDestroyed

    /** [FViewModel.isVMActive] */
    protected val isVMActive get() = vm.isVMActive

    /** [FViewModel.isActiveFlow] */
    protected val isActiveFlow get() = vm.isActiveFlow

    /** [FViewModel.singleDispatcher] */
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
     * [PluginViewModel.Plugin.notifyInit]
     */
    protected open fun onInit() {}

    /**
     * [PluginViewModel.Plugin.notifyDestroy]
     */
    protected open fun onDestroy() {}
}

private fun checkMainThread() {
    check(Looper.getMainLooper() === Looper.myLooper()) {
        "Expected main thread but was " + Thread.currentThread().name
    }
}