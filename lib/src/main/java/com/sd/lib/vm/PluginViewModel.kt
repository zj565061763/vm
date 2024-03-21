package com.sd.lib.vm

import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.lifecycle.viewModelScope
import com.sd.lib.vm.plugin.VMPlugin
import com.sd.lib.vm.plugin.VMPluginManager
import kotlinx.coroutines.CoroutineScope

open class PluginViewModel<I> : FViewModel<I>() {
    private val _pluginManager = VMPluginManager(newPluginSupport())

    /**
     * 注册插件
     */
    @MainThread
    protected fun <T : VMPlugin> T.register(): T {
        return this.also {
            _pluginManager.registerPlugin(it)
        }
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        _pluginManager.notifyDestroy()
    }

    private fun newPluginSupport(): VMPlugin.Support {
        return object : VMPlugin.Support {
            override val vmScope: CoroutineScope get() = this@PluginViewModel.viewModelScope
            override val isVMDestroyed: Boolean get() = this@PluginViewModel.isVMDestroyed
            override val isVMActive: Boolean get() = this@PluginViewModel.isVMActive
            override fun registerPlugin(plugin: VMPlugin) = _pluginManager.registerPlugin(plugin)
        }
    }
}