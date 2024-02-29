package com.sd.lib.vm.plugin

import com.sd.lib.vm.PluginViewModel

interface StatePlugin<T> : PluginViewModel.Plugin, StateOwner<T>