package com.sd.lib.vm.plugin

import com.sd.lib.coroutine.FMutator
import com.sd.lib.vm.FViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 加载数据
 */
interface LoadPlugin : StatePlugin<StateFlow<LoadState>> {
    /**
     * 加载数据，如果上一次加载还未完成，再次调用此方法，会取消上一次加载
     *
     * @param notifyLoading 是否通知[LoadState.isLoading]
     * @param ignoreActive 是否忽略激活状态[FViewModel.isVMActive]
     * @param canLoad 是否可以加载
     * @param onLoad 加载回调
     */
    fun load(
        notifyLoading: Boolean = true,
        ignoreActive: Boolean = false,
        canLoad: suspend () -> Boolean = { true },
        onLoad: suspend () -> Unit,
    )

    /**
     * 取消加载
     */
    fun cancelLoad()
}

/**
 * [LoadPlugin]
 */
fun LoadPlugin(): LoadPlugin {
    return LoadPluginImpl()
}

//---------- state ----------

data class LoadState(
    /** 是否正在加载中 */
    val isLoading: Boolean = false,
)

//---------- impl ----------

private class LoadPluginImpl : ViewModelPlugin(), LoadPlugin {

    /** 互斥修改器 */
    private val _mutator = FMutator()

    private val _state = MutableStateFlow(LoadState())
    override val state: StateFlow<LoadState> = _state.asStateFlow()

    override fun load(
        notifyLoading: Boolean,
        ignoreActive: Boolean,
        canLoad: suspend () -> Boolean,
        onLoad: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            loadInternal(
                notifyLoading = notifyLoading,
                ignoreActive = ignoreActive,
                canLoad = canLoad,
                onLoad = onLoad,
            )
        }
    }

    override fun cancelLoad() {
        _mutator.cancel()
    }

    private suspend fun loadInternal(
        notifyLoading: Boolean,
        ignoreActive: Boolean,
        canLoad: suspend () -> Boolean,
        onLoad: suspend () -> Unit,
    ) {
        if (isVMDestroyed) return
        if (isVMActive || ignoreActive) {
            if (canLoad()) {
                try {
                    _mutator.mutate {
                        if (notifyLoading) {
                            _state.update { it.copy(isLoading = true) }
                        }
                        onLoad()
                    }
                } finally {
                    if (notifyLoading) {
                        _state.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }
}