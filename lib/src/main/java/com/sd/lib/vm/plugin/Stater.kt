package com.sd.lib.vm.plugin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface StateOwner<T> {
    /** 状态 */
    val state: StateFlow<T>
}

interface StateUpdater<T> {
    /**
     * 把[function]的返回值更新为状态
     */
    fun update(function: (T) -> T)
}

/**
 * 状态管理
 */
interface Stater<T> : StateOwner<T>, StateUpdater<T>

/**
 * [Stater]
 */
fun <T> Stater(initial: T): Stater<T> = StaterImpl(initial)

private class StaterImpl<T>(initial: T) : Stater<T> {
    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<T> = _state.asStateFlow()
    override fun update(function: (T) -> T) = _state.update(function)
}