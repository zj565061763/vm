package com.sd.lib.vm.plugin

import android.os.Looper
import androidx.annotation.MainThread
import androidx.lifecycle.viewModelScope
import com.sd.lib.vm.PluginViewModel
import kotlinx.coroutines.flow.StateFlow

interface StatePlugin<T> : PluginViewModel.Plugin {
    /** 状态 */
    val state: StateFlow<T>
}

abstract class ViewModelPlugin : PluginViewModel.Plugin {
    private var _vm: PluginViewModel<*>? = null

    protected val vm get() = checkNotNull(_vm) { "Plugin has not been initialized." }

    protected val viewModelScope get() = vm.viewModelScope
    protected val isDestroyed get() = vm.isDestroyed
    protected val isVMActive get() = vm.isVMActive
    protected val isActiveFlow get() = vm.isActiveFlow
    protected val singleDispatcher get() = vm.singleDispatcher

    /**
     * 通知初始化
     */
    @MainThread
    internal fun notifyInit(viewModel: PluginViewModel<*>) {
        checkMainThread()
        if (_vm != null) error("Plugin has been initialized.")
        _vm = viewModel
        onInit()
    }

    /**
     * 通知销毁
     */
    @MainThread
    internal fun notifyDestroy() {
        checkMainThread()
        onDestroy()
    }

    /**
     * 注册插件
     */
    @MainThread
    protected fun <T : PluginViewModel.Plugin> T.register() {
        if (this === this@ViewModelPlugin) error("Can not register self.")
        vm.registerPlugin(this)
    }

    /**
     * 初始化
     */
    @MainThread
    protected open fun onInit() = Unit

    /**
     * 销毁
     */
    @MainThread
    protected open fun onDestroy() = Unit
}

private fun checkMainThread() {
    check(Looper.myLooper() === Looper.getMainLooper()) {
        "Expected main thread but was " + Thread.currentThread().name
    }
}