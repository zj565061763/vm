package com.sd.lib.vm.plugin

import com.sd.lib.vm.PluginViewModel
import kotlinx.coroutines.flow.StateFlow

interface StatePlugin<T> : PluginViewModel.Plugin {
    /** 状态 */
    val state: StateFlow<T>
}