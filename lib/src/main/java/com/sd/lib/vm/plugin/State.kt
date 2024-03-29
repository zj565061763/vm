package com.sd.lib.vm.plugin

interface StateOwner<T> {
    /** 状态 */
    val state: T
}

interface StateVMPlugin<T> : VMPlugin, StateOwner<T>