package com.sd.lib.vm.plugin

import android.os.Looper
import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class VMPluginManager(
    private val support: RealVMPlugin.Support
) {
    private val _plugins: MutableSet<RealVMPlugin> = hashSetOf()

    /**
     * 注册插件
     */
    @MainThread
    fun registerPlugin(plugin: VMPlugin) {
        if (support.isVMDestroyed) return
        require(plugin is RealVMPlugin) { "Require ${RealVMPlugin::class.java.simpleName}" }
        if (_plugins.add(plugin)) {
            plugin.notifyInit(support)
        }
    }

    /**
     * 销毁所有插件
     */
    @MainThread
    fun destroyPlugins() {
        _plugins.forEach { it.notifyDestroy() }
        _plugins.clear()
    }
}

interface VMPlugin

abstract class RealVMPlugin : VMPlugin {
    private var _support: Support? = null
    private val support get() = checkNotNull(_support) { "Plugin has not been initialized." }

    /**
     * 通知初始化
     */
    @MainThread
    internal fun notifyInit(support: Support) {
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

    protected val vmScope get() = support.vmScope
    protected val isVMDestroyed get() = support.isVMDestroyed
    protected val isVMActive get() = support.isVMActive
    protected val isVMActiveFlow get() = support.isVMActiveFlow
    protected fun registerPlugin(plugin: VMPlugin) = support.registerPlugin(plugin)

    interface Support {
        val vmScope: CoroutineScope
        val isVMDestroyed: Boolean
        val isVMActive: Boolean
        val isVMActiveFlow: Flow<Boolean>
        fun registerPlugin(plugin: VMPlugin)
    }
}

private fun checkMainThread() {
    check(Looper.myLooper() === Looper.getMainLooper()) {
        "Expected main thread but was " + Thread.currentThread().name
    }
}