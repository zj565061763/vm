package com.sd.lib.vm.plugin

import com.sd.lib.coroutine.FMutator
import com.sd.lib.vm.AbsViewModelPlugin
import com.sd.lib.vm.FViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DataState<T>(
    /** 数据 */
    val data: T? = null,

    /** 数据结果 */
    val result: Result<Unit>? = null,

    /** 是否正在加载中 */
    val isLoading: Boolean = false,
)

interface DataPlugin<T> : StatePlugin<DataState<T>> {
    /**
     * 加载数据，如果上一次请求还未完成，再次调用此方法，则上一次的请求会被取消
     *
     * @param notifyLoading 是否通知[DataState.isLoading]
     * @param ignoreActive 是否忽略激活状态[FViewModel.isActiveFlow]
     */
    fun load(
        notifyLoading: Boolean = true,
        ignoreActive: Boolean = false,
    )

    /**
     * 取消加载
     */
    fun cancelLoad()
}

fun <T> DataPlugin(
    /** 状态管理 */
    stater: Stater<DataState<T>> = Stater(DataState()),
    /** 加载回调 */
    onLoad: suspend () -> Result<T?>,
): DataPlugin<T> {
    return DataPluginImpl(
        stater = stater,
        onLoad = onLoad,
    )
}

private class DataPluginImpl<T>(
    /** 状态管理 */
    private val stater: Stater<DataState<T>>,
    /** 加载回调 */
    private val onLoad: suspend () -> Result<T?>,
) : AbsViewModelPlugin(), DataPlugin<T> {

    /** 互斥修改器 */
    private val _mutator = FMutator()

    override val state: StateFlow<DataState<T>>
        get() = stater.state

    override fun load(
        notifyLoading: Boolean,
        ignoreActive: Boolean,
    ) {
        viewModelScope.launch {
            loadInternal(
                notifyLoading = notifyLoading,
                ignoreActive = ignoreActive,
            )
        }
    }

    override fun cancelLoad() {
        _mutator.cancel()
    }

    private suspend fun loadInternal(
        notifyLoading: Boolean,
        ignoreActive: Boolean,
    ) {
        if (isDestroyed) return
        if (isVMActive || ignoreActive) {
            try {
                _mutator.mutate {
                    if (notifyLoading) {
                        stater.update { it.copy(isLoading = true) }
                    }
                    val result = onLoad()
                    handleLoadResult(result)
                }
            } finally {
                if (notifyLoading) {
                    stater.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun handleLoadResult(result: Result<T?>) {
        result.onSuccess { data ->
            stater.update {
                it.copy(
                    result = Result.success(Unit),
                    data = data,
                )
            }
        }

        result.onFailure { throwable ->
            stater.update {
                it.copy(result = Result.failure(throwable))
            }
        }
    }
}