package com.sd.lib.vm.plugin

import com.sd.lib.vm.FViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 加载数据
 */
interface DataPlugin<T> : StatePlugin<StateFlow<DataState<T>>> {
    /**
     * 加载数据，如果上一次加载还未完成，再次调用此方法，会取消上一次加载
     *
     * @param notifyLoading 是否通知[DataState.isLoading]
     * @param ignoreActive 是否忽略激活状态[FViewModel.isVMActive]
     * @param canLoad 是否可以加载
     * @param onLoad 加载回调
     */
    fun load(
        notifyLoading: Boolean = true,
        ignoreActive: Boolean = false,
        canLoad: suspend LoadScope<T>.() -> Boolean = { true },
        onLoad: suspend LoadScope<T>.() -> Result<T>,
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
 */
fun <T> DataPlugin(initial: T): DataPlugin<T> {
    return DataPluginImpl(initial = initial)
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

/**
 * 加载中状态
 */
inline fun <T> DataState<T>.onLoading(action: DataState<T>.() -> Unit): DataState<T> {
    if (isLoading) action()
    return this
}

//---------- impl ----------

private class DataPluginImpl<T>(initial: T) : ViewModelPlugin(), DataPlugin<T> {

    private val _loadPlugin = LoadPlugin()

    private val _state = MutableStateFlow(DataState(data = initial))
    override val state: StateFlow<DataState<T>> = _state.asStateFlow()

    private val _loadScopeImpl = object : DataPlugin.LoadScope<T> {
        override val currentState: DataState<T>
            get() = this@DataPluginImpl.state.value
    }

    override fun load(
        notifyLoading: Boolean,
        ignoreActive: Boolean,
        canLoad: suspend DataPlugin.LoadScope<T>.() -> Boolean,
        onLoad: suspend DataPlugin.LoadScope<T>.() -> Result<T>,
    ) {
        _loadPlugin.load(
            notifyLoading = notifyLoading,
            ignoreActive = ignoreActive,
            canLoad = { with(_loadScopeImpl) { canLoad() } },
        ) {
            val result = with(_loadScopeImpl) { onLoad() }
            handleLoadResult(result)
        }
    }

    override fun cancelLoad() {
        _loadPlugin.cancelLoad()
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

    override fun onInit() {
        super.onInit()
        _loadPlugin.register()

        viewModelScope.launch {
            _loadPlugin.state
                .map { it.isLoading }
                .distinctUntilChanged()
                .collect { isLoading ->
                    _state.update {
                        it.copy(isLoading = isLoading)
                    }
                }
        }
    }
}