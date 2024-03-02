package com.sd.lib.vm.plugin

import com.sd.lib.vm.PluginViewModel

interface StateOwner<T> {
    /** 状态 */
    val state: T
}

interface StatePlugin<T> : PluginViewModel.Plugin, StateOwner<T>