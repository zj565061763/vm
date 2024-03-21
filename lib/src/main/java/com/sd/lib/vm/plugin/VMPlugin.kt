package com.sd.lib.vm.plugin

import android.os.Looper
import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineScope

class VMPluginManager(
    private val support: VMPlugin.Support
) {
    private val _plugins: MutableSet<RealVMPlugin> = hashSetOf()

    /**
     * 注册插件，[plugin]必须是[RealVMPlugin]的实现类
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
     * 通知销毁，销毁所有插件
     */
    @MainThread
    fun notifyDestroy() {
        _plugins.forEach { it.notifyDestroy() }
        _plugins.clear()
    }
}

interface VMPlugin {
    interface Support {
        val vmScope: CoroutineScope
        val isVMDestroyed: Boolean
        val isVMActive: Boolean
        fun registerPlugin(plugin: VMPlugin)
    }
}

abstract class RealVMPlugin : VMPlugin {
    private var _support: VMPlugin.Support? = null
    private val support get() = checkNotNull(_support) { "Plugin has not been initialized." }

    /**
     * 通知初始化
     */
    internal fun notifyInit(support: VMPlugin.Support) {
        checkMainThread()
        if (_support != null) error("Plugin has been initialized.")
        _support = support
        onInit()
    }

    /**
     * 通知销毁
     */
    internal fun notifyDestroy() {
        checkMainThread()
        onDestroy()
    }

    /**
     * 初始化(MainThread)
     */
    protected open fun onInit() = Unit

    /**
     * 销毁(MainThread)
     */
    protected open fun onDestroy() = Unit

    //-------------------- Support --------------------

    protected val vmScope get() = support.vmScope
    protected val isVMDestroyed get() = support.isVMDestroyed
    protected val isVMActive get() = support.isVMActive
    protected fun registerPlugin(plugin: VMPlugin) = support.registerPlugin(plugin)
}

private fun checkMainThread() {
    check(Looper.myLooper() === Looper.getMainLooper()) {
        "Expected main thread but was " + Thread.currentThread().name
    }
}