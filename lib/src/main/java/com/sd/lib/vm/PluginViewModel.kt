package com.sd.lib.vm

import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import com.sd.lib.vm.plugin.ViewModelPlugin

abstract class PluginViewModel<I> : FViewModel<I>() {

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
        check(plugin is ViewModelPlugin)
        if (_plugins.add(plugin)) {
            plugin.notifyInit(this@PluginViewModel)
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

    interface Plugin
}