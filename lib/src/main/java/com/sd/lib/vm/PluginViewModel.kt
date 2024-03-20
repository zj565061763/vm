package com.sd.lib.vm

import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.lifecycle.viewModelScope
import com.sd.lib.vm.plugin.ViewModelPlugin
import com.sd.lib.vm.plugin.ViewModelPluginSupport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

open class PluginViewModel<I> : FViewModel<I>() {

    private val _plugins: MutableSet<ViewModelPlugin> = hashSetOf()

    /**
     * 注册插件
     */
    @MainThread
    protected fun <T : Plugin> T.register(): T {
        return this.also { registerPlugin(it) }
    }

    @MainThread
    internal fun registerPlugin(plugin: Plugin) {
        if (isDestroyed) return
        require(plugin is ViewModelPlugin) { "plugin should be ${ViewModelPlugin::class.java.name}" }
        if (_plugins.add(plugin)) {
            plugin.notifyInit(_pluginSupport)
        }
    }

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

    private val _pluginSupport = object : ViewModelPluginSupport {
        override val viewModelScope: CoroutineScope get() = this@PluginViewModel.viewModelScope
        override val isDestroyed: Boolean get() = this@PluginViewModel.isDestroyed
        override val isVMActive: Boolean get() = this@PluginViewModel.isVMActive
        override val isActiveFlow: Flow<Boolean> get() = this@PluginViewModel.isActiveFlow
        override fun registerPlugin(plugin: Plugin) = this@PluginViewModel.registerPlugin(plugin)
    }

    interface Plugin
}