package com.sd.lib.vm

import android.os.Looper
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.lifecycle.viewModelScope

abstract class PluginViewModel<I> : FViewModel<I>() {

    private val _plugins: MutableSet<Plugin> = hashSetOf()

    /**
     * 注册插件(MainThread)
     */
    @MainThread
    fun <T : Plugin> T.register(): T {
        return this.also { plugin ->
            if (!isDestroyed) {
                if (_plugins.add(plugin)) {
                    plugin.notifyInit(this@PluginViewModel)
                }
            }
        }
    }

    /**
     * 销毁并清空插件
     */
    @MainThread
    private fun destroyPlugins() {
        if (isDestroyed) {
            _plugins.forEach { it.notifyDestroy() }
            _plugins.clear()
        }
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        destroyPlugins()
    }

    interface Plugin {
        /**
         * 初始化(MainThread)
         */
        @MainThread
        fun notifyInit(viewModel: PluginViewModel<*>)

        /**
         * 销毁(MainThread)
         */
        @MainThread
        fun notifyDestroy()
    }
}

abstract class AbsViewModelPlugin : PluginViewModel.Plugin {
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