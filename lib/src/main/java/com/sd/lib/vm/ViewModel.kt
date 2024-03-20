package com.sd.lib.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * 忽略激活状态的意图
 */
interface IgnoreActiveIntent

/**
 * 有激活状态的[ViewModel]，默认激活状态为true，可以通过[setActive]来改变激活状态。
 * 当激活状态为false的时候，不处理意图[I]，除非意图是[IgnoreActiveIntent]类型的。
 */
open class FViewModel<I> : ViewModel() {

    /** 是否已经销毁 */
    @Volatile
    var isVMDestroyed: Boolean = false
        private set(value) {
            require(value) { "Require true value." }
            field = true
        }

    @Volatile
    private var _isVMActive = true
    private val _isVMActiveFlow = MutableStateFlow(_isVMActive)

    /** 是否处于激活状态 */
    val isVMActive: Boolean get() = _isVMActive

    /** 基于[Dispatchers.Default]并发为1的调度器 */
    @OptIn(ExperimentalCoroutinesApi::class)
    val singleDispatcher: CoroutineDispatcher by lazy { Dispatchers.Default.limitedParallelism(1) }

    /**
     * 设置激活状态
     */
    fun setActive(active: Boolean) {
        if (isVMDestroyed) return
        _isVMActive = active
        _isVMActiveFlow.value = active
    }

    /**
     * 触发意图
     */
    fun dispatch(intent: I) {
        if (isVMDestroyed) return
        viewModelScope.launch {
            if (canDispatchIntent(intent)) {
                handleIntent(intent)
            }
        }
    }

    /**
     * 是否可以触发意图，[viewModelScope]触发
     */
    protected open suspend fun canDispatchIntent(intent: I): Boolean {
        return isVMActive || intent is IgnoreActiveIntent
    }

    /**
     * 处理意图，[viewModelScope]触发
     */
    protected open suspend fun handleIntent(intent: I) = Unit

    /**
     * 未激活 -> 激活 (MainThread)
     */
    protected open fun onActive() = Unit

    /**
     * 激活 -> 未激活 (MainThread)
     */
    protected open fun onInActive() = Unit

    /**
     * 销毁回调
     */
    protected open fun onDestroy() = Unit

    final override fun onCleared() {
        super.onCleared()
        isVMDestroyed = true
        _isVMActive = false
        _isVMActiveFlow.value = false
        singleDispatcher.cancel()
        onDestroy()
    }

    init {
        viewModelScope.launch {
            _isVMActiveFlow
                .drop(1)
                .collect { active ->
                    if (active) onActive() else onInActive()
                }
        }
    }
}