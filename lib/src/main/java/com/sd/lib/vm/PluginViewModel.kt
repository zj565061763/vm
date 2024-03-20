package com.sd.lib.vm

import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.lifecycle.viewModelScope
import com.sd.lib.vm.plugin.VMPlugin
import com.sd.lib.vm.plugin.ViewModelPluginSupport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

open class PluginViewModel<I> : FViewModel<I>() {

    private val _plugins: MutableSet<VMPlugin> = hashSetOf()

    /**
     * 注册插件
     */
    @MainThread
    protected fun <T : Plugin> T.register(): T {
        return this.also { registerPlugin(it) }
    }

    @MainThread
    internal fun registerPlugin(plugin: Plugin) {
        if (isVMDestroyed) return
        require(plugin is VMPlugin) { "plugin should be ${VMPlugin::class.java.name}" }
        if (_plugins.add(plugin)) {
            plugin.notifyInit(_pluginSupport)
        }
    }

    @MainThread
    private fun destroyPlugins() {
        if (isVMDestroyed) {
            _plugins.forEach { it.notifyDestroy() }
            _plugins.clear()
        }
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        destroyPlugins()
    }

    private val _pluginSupport = object : ViewModelPluginSupport {
        override val viewModelScope: CoroutineScope get() = this@PluginViewModel.viewModelScope
        override val isVMDestroyed: Boolean get() = this@PluginViewModel.isVMDestroyed
        override val isVMActive: Boolean get() = this@PluginViewModel.isVMActive
        override val isVMActiveFlow: Flow<Boolean> get() = this@PluginViewModel.isVMActiveFlow
        override fun registerPlugin(plugin: Plugin) = this@PluginViewModel.registerPlugin(plugin)
    }

    interface Plugin
}