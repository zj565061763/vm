package com.sd.lib.vm.plugin

import android.os.Looper
import androidx.annotation.MainThread
import com.sd.lib.vm.PluginViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface ViewModelPluginSupport {
    val viewModelScope: CoroutineScope
    val isDestroyed: Boolean
    val isVMActive: Boolean
    val isActiveFlow: Flow<Boolean>
    fun registerPlugin(plugin: PluginViewModel.Plugin)
}

abstract class ViewModelPlugin : PluginViewModel.Plugin, ViewModelPluginSupport {
    private var _support: ViewModelPluginSupport? = null
    private val support get() = checkNotNull(_support) { "Plugin has not been initialized." }

    override val viewModelScope get() = support.viewModelScope
    override val isDestroyed get() = support.isDestroyed
    override val isVMActive get() = support.isVMActive
    override val isActiveFlow get() = support.isActiveFlow

    @MainThread
    override fun registerPlugin(plugin: PluginViewModel.Plugin) {
        require(plugin !== this@ViewModelPlugin)
        support.registerPlugin(plugin)
    }

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
}

private fun checkMainThread() {
    check(Looper.myLooper() === Looper.getMainLooper()) {
        "Expected main thread but was " + Thread.currentThread().name
    }
}