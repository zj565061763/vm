package com.sd.lib.vm.plugin

import android.os.Looper
import androidx.annotation.MainThread
import com.sd.lib.vm.PluginViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface ViewModelPluginSupport {
    val viewModelScope: CoroutineScope
    val isVMDestroyed: Boolean
    val isVMActive: Boolean
    val isVMActiveFlow: Flow<Boolean>
    fun registerPlugin(plugin: PluginViewModel.Plugin)
}

abstract class VMPlugin : PluginViewModel.Plugin {
    private var _support: ViewModelPluginSupport? = null
    private val support get() = checkNotNull(_support) { "Plugin has not been initialized." }

    /**
     * 通知初始化
     */
    @MainThread
    internal fun notifyInit(support: ViewModelPluginSupport) {
        checkMainThread()
        if (_support != null) error("Plugin has been initialized.")
        _support = support
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
     * 初始化
     */
    @MainThread
    protected open fun onInit() = Unit

    /**
     * 销毁
     */
    @MainThread
    protected open fun onDestroy() = Unit

    //-------------------- Support --------------------

    protected val viewModelScope get() = support.viewModelScope
    protected val isVMDestroyed get() = support.isVMDestroyed
    protected val isVMActive get() = support.isVMActive
    protected val isVMActiveFlow get() = support.isVMActiveFlow
    protected fun registerPlugin(plugin: PluginViewModel.Plugin) = support.registerPlugin(plugin)
}

private fun checkMainThread() {
    check(Looper.myLooper() === Looper.getMainLooper()) {
        "Expected main thread but was " + Thread.currentThread().name
    }
}