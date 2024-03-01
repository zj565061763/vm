package com.sd.lib.vm.plugin

import com.sd.lib.coroutine.FMutator
import com.sd.lib.vm.FViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 数据加载
 */
interface DataPlugin<T> : StatePlugin<DataState<T>> {
    /**
     * 加载数据，如果上一次加载还未完成，再次调用此方法，会取消上一次加载
     *
     * @param notifyLoading 是否通知[DataState.isLoading]
     * @param ignoreActive 是否忽略激活状态[FViewModel.isVMActive]
     */
    fun load(
        notifyLoading: Boolean = true,
        ignoreActive: Boolean = false,
    )

    /**
     * 取消加载
     */
    fun cancelLoad()

    /**
     * 更新数据
     */
    fun update(function: (T) -> T)

    interface LoadScope<T> {
        val currentState: DataState<T>
    }
}

/**
 * [DataPlugin]
 *
 * @param initial 初始值
 * @param canLoad 是否可以加载数据
 * @param onLoad 数据加载回调
 */
fun <T> DataPlugin(
    initial: T,
    canLoad: suspend DataPlugin.LoadScope<T>.() -> Boolean = { true },
    onLoad: suspend DataPlugin.LoadScope<T>.() -> Result<T>,
): DataPlugin<T> {
    return DataPluginImpl(
        initial = initial,
        canLoad = canLoad,
        onLoad = onLoad,
    )
}

//---------- state ----------

data class DataState<T>(
    /** 数据 */
    val data: T,

    /** 数据结果 */
    val result: Result<Unit>? = null,

    /** 是否正在加载中 */
    val isLoading: Boolean = false,
)

/** 是否初始状态 */
val DataState<*>.isInitial: Boolean get() = result == null

/** 是否成功状态 */
val DataState<*>.isSuccess: Boolean get() = result?.isSuccess == true

/** 是否失败状态 */
val DataState<*>.isFailure: Boolean get() = result?.isFailure == true

/**
 * 初始状态
 */
inline fun <T> DataState<T>.onInitial(action: (data: T) -> Unit): DataState<T> {
    if (result == null) action(data)
    return this
}

/**
 * 成功状态
 */
inline fun <T> DataState<T>.onSuccess(action: (data: T) -> Unit): DataState<T> {
    result?.onSuccess { action(data) }
    return this
}

/**
 * 失败状态
 */
inline fun <T> DataState<T>.onFailure(action: (exception: Throwable) -> Unit): DataState<T> {
    result?.onFailure { action(it) }
    return this
}

//---------- impl ----------

private class DataPluginImpl<T>(
    initial: T,
    private val canLoad: suspend DataPlugin.LoadScope<T>.() -> Boolean,
    private val onLoad: suspend DataPlugin.LoadScope<T>.() -> Result<T>,
) : ViewModelPlugin(), DataPlugin<T> {

    /** 互斥修改器 */
    private val _mutator = FMutator()

    private val _state = MutableStateFlow(DataState(data = initial))
    override val state: StateFlow<DataState<T>> = _state.asStateFlow()

    private val _loadScopeImpl = object : DataPlugin.LoadScope<T> {
        override val currentState: DataState<T>
            get() = this@DataPluginImpl.state.value
    }

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

    override fun update(function: (T) -> T) {
        _state.update {
            it.copy(data = function(it.data))
        }
    }

    private suspend fun loadInternal(
        notifyLoading: Boolean,
        ignoreActive: Boolean,
    ) {
        if (isDestroyed) return
        if (isVMActive || ignoreActive) {
            val canLoad = with(_loadScopeImpl) { canLoad() }
            if (canLoad) {
                try {
                    _mutator.mutate {
                        if (notifyLoading) {
                            _state.update { it.copy(isLoading = true) }
                        }
                        val result = with(_loadScopeImpl) { onLoad() }
                        handleLoadResult(result)
                    }
                } finally {
                    if (notifyLoading) {
                        _state.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    private fun handleLoadResult(result: Result<T>) {
        result.onSuccess { data ->
            _state.update {
                it.copy(
                    data = data,
                    result = Result.success(Unit),
                )
            }
        }

        result.onFailure { throwable ->
            _state.update {
                it.copy(result = Result.failure(throwable))
            }
        }
    }
}