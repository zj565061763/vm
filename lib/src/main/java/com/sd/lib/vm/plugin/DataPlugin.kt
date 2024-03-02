package com.sd.lib.vm.plugin

import com.sd.lib.coroutine.FMutator
import com.sd.lib.vm.FViewModel
import com.sd.lib.vm.PluginViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 数据加载
 */
interface DataPlugin<T> : PluginViewModel.Plugin {

    /** 状态 */
    val state: StateFlow<DataState<T>>

    /**
     * 加载数据，如果上一次加载还未完成，再次调用此方法，会取消上一次加载
     *
     * @param notifyLoading 是否通知[DataState.isLoading]
     * @param ignoreActive 是否忽略激活状态[FViewModel.isVMActive]
     * @param canLoad 是否可以加载数据
     */
    fun load(
        notifyLoading: Boolean = true,
        ignoreActive: Boolean = false,
        canLoad: (suspend LoadScope<T>.() -> Boolean)? = null,
    )

    /**
     * 取消加载
     */
    fun cancelLoad()

    interface LoadScope<T> {
        /** 当前数据状态 */
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
inline fun <T> DataState<T>.onInitial(action: DataState<T>.() -> Unit): DataState<T> {
    if (result == null) action()
    return this
}

/**
 * 成功状态
 */
inline fun <T> DataState<T>.onSuccess(action: DataState<T>.() -> Unit): DataState<T> {
    result?.onSuccess { action() }
    return this
}

/**
 * 失败状态
 */
inline fun <T> DataState<T>.onFailure(action: DataState<T>.(exception: Throwable) -> Unit): DataState<T> {
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
        canLoad: (suspend DataPlugin.LoadScope<T>.() -> Boolean)?,
    ) {
        viewModelScope.launch {
            loadData(
                notifyLoading = notifyLoading,
                ignoreActive = ignoreActive,
                canLoad = canLoad,
            )
        }
    }

    override fun cancelLoad() {
        _mutator.cancel()
    }

    /**
     * 加载数据
     */
    private suspend fun loadData(
        notifyLoading: Boolean,
        ignoreActive: Boolean,
        canLoad: (suspend DataPlugin.LoadScope<T>.() -> Boolean)?,
    ) {
        if (isDestroyed) return
        if (isVMActive || ignoreActive) {
            val canLoadBlock = canLoad ?: this@DataPluginImpl.canLoad
            if (with(_loadScopeImpl) { canLoadBlock() }) {
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

    /**
     * 处理加载结果
     */
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