package com.sd.lib.vm

import androidx.annotation.CallSuper
import androidx.annotation.MainThread

abstract class PluginViewModel<I> : FViewModel<I>() {

    private val _plugins: MutableSet<Plugin> = hashSetOf()

    /**
     * 注册插件
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
         * 初始化
         */
        @MainThread
        fun notifyInit(viewModel: PluginViewModel<*>)

        /**
         * 销毁
         */
        @MainThread
        fun notifyDestroy()
    }
}