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
 * 分页管理
 */
interface PagePlugin<T> : StatePlugin<PageState<T>> {

    /** 刷新数据的页码，例如数据源规定页码从1开始，那么此参数就为1 */
    val refreshPage: Int

    /**
     * 刷新数据
     *
     * @param notifyLoading 是否通知[PageState.isRefreshing]
     * @param ignoreActive 是否忽略激活状态[FViewModel.isVMActive]
     * @param canLoad 是否可以加载数据
     */
    fun refresh(
        notifyLoading: Boolean = true,
        ignoreActive: Boolean = false,
        canLoad: (suspend LoadScope<T>.(page: Int) -> Boolean)? = null,
    )

    /**
     * 加载更多数据
     *
     * @param notifyLoading 是否通知[PageState.isLoadingMore]
     * @param ignoreActive 是否忽略激活状态[FViewModel.isVMActive]
     * @param canLoad 是否可以加载数据
     */
    fun loadMore(
        notifyLoading: Boolean = true,
        ignoreActive: Boolean = false,
        canLoad: (suspend LoadScope<T>.(page: Int) -> Boolean)? = null,
    )

    interface LoadScope<T> {
        /** 当前数据状态 */
        val currentState: PageState<T>

        /** 刷新数据的页码，例如数据源规定页码从1开始，那么此参数就为1 */
        val refreshPage: Int
    }

    data class Data<T>(
        /** 所有页的数据 */
        val data: List<T>,

        /** 本页实际加载到的数据个数 */
        val pageSize: Int,

        /** 是否还有更多数据 */
        val hasMore: Boolean = false,
    )
}

/**
 * [PagePlugin]
 *
 * @param initial 初始值
 * @param refreshPage 刷新数据的页码，例如数据源规定页码从1开始，那么此参数就为1
 * @param canLoad 是否可以加载数据
 * @param onLoad 数据加载回调
 */
fun <T> PagePlugin(
    initial: List<T> = emptyList(),
    refreshPage: Int = 1,
    canLoad: suspend PagePlugin.LoadScope<T>.(page: Int) -> Boolean = { page ->
        if (page == this.refreshPage) {
            true
        } else {
            !currentState.isLoadingMore
        }
    },
    onLoad: suspend PagePlugin.LoadScope<T>.(page: Int) -> Result<PagePlugin.Data<T>>,
): PagePlugin<T> {
    return PagePluginImpl(
        initial = initial,
        refreshPage = refreshPage,
        canLoad = canLoad,
        onLoad = onLoad,
    )
}

//---------- state ----------

data class PageState<T>(
    /** 所有页的数据 */
    val data: List<T>,

    /** 当前页码 */
    val page: Int = 0,

    /** 当前页码的数据结果 */
    val result: Result<Unit>? = null,

    /** 是否还有更多数据 */
    val hasMore: Boolean = false,

    /** 是否正在刷新 */
    val isRefreshing: Boolean = false,

    /** 是否正在加载更多 */
    val isLoadingMore: Boolean = false,
)

/** 是否初始状态 */
val PageState<*>.isInitial: Boolean get() = result == null

/** 是否成功状态 */
val PageState<*>.isSuccess: Boolean get() = result?.isSuccess == true

/** 是否失败状态 */
val PageState<*>.isFailure: Boolean get() = result?.isFailure == true

/**
 * 初始状态
 */
inline fun <T> PageState<T>.onInitial(action: PageState<T>.() -> Unit): PageState<T> {
    if (result == null) action()
    return this
}

/**
 * 成功状态
 */
inline fun <T> PageState<T>.onSuccess(action: PageState<T>.() -> Unit): PageState<T> {
    result?.onSuccess { action() }
    return this
}

/**
 * 失败状态
 */
inline fun <T> PageState<T>.onFailure(action: PageState<T>.(exception: Throwable) -> Unit): PageState<T> {
    result?.onFailure { action(it) }
    return this
}

//---------- impl ----------

private class PagePluginImpl<T>(
    initial: List<T>,
    override val refreshPage: Int,
    private val canLoad: suspend PagePlugin.LoadScope<T>.(page: Int) -> Boolean,
    private val onLoad: suspend PagePlugin.LoadScope<T>.(page: Int) -> Result<PagePlugin.Data<T>>,
) : ViewModelPlugin(), PagePlugin<T> {

    private val _refreshPlugin = DataPlugin(Unit) {
        val page = refreshPage
        loadData(page)
    }

    private val _loadMorePlugin = DataPlugin(Unit) {
        val page = state.value.page + 1
        loadData(page)
    }

    private val _state = MutableStateFlow(PageState(data = initial))
    override val state: StateFlow<PageState<T>> = _state.asStateFlow()

    private val _loadScopeImpl = object : PagePlugin.LoadScope<T> {
        override val currentState: PageState<T> get() = this@PagePluginImpl.state.value
        override val refreshPage: Int get() = this@PagePluginImpl.refreshPage
    }

    override fun refresh(
        notifyLoading: Boolean,
        ignoreActive: Boolean,
        canLoad: (suspend PagePlugin.LoadScope<T>.(page: Int) -> Boolean)?,
    ) {
        _refreshPlugin.load(
            notifyLoading = notifyLoading,
            ignoreActive = ignoreActive,
            canLoad = {
                canLoadData(
                    page = refreshPage,
                    canLoad = canLoad,
                ).also {
                    if (it) {
                        // 刷新之前取消加载更多
                        _loadMorePlugin.cancelLoad()
                    }
                }
            },
        )
    }

    override fun loadMore(
        notifyLoading: Boolean,
        ignoreActive: Boolean,
        canLoad: (suspend PagePlugin.LoadScope<T>.(page: Int) -> Boolean)?,
    ) {
        _loadMorePlugin.load(
            notifyLoading = notifyLoading,
            ignoreActive = ignoreActive,
            canLoad = {
                canLoadData(
                    page = state.value.page + 1,
                    canLoad = canLoad,
                )
            },
        )
    }

    /**
     * 是否可以加载数据
     */
    private suspend fun canLoadData(
        page: Int,
        canLoad: (suspend PagePlugin.LoadScope<T>.(page: Int) -> Boolean)?,
    ): Boolean {
        val canLoadBlock = canLoad ?: this@PagePluginImpl.canLoad
        return with(_loadScopeImpl) { canLoadBlock(page) }
    }

    /**
     * 加载数据
     */
    private suspend fun loadData(page: Int): Result<Unit> {
        val result = with(_loadScopeImpl) { onLoad(page) }
        handleLoadResult(result, page)
        return result.map { }
    }

    /**
     * 处理加载结果
     */
    private fun handleLoadResult(result: Result<PagePlugin.Data<T>>, page: Int) {
        result.onSuccess { data ->
            val newPage = if (page == refreshPage) {
                // refresh
                refreshPage
            } else {
                // loadMore
                if (data.pageSize > 0) page else page - 1
            }

            _state.update {
                it.copy(
                    data = data.data,
                    page = newPage,
                    result = Result.success(Unit),
                    hasMore = data.hasMore,
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
        _refreshPlugin.notifyInit(vm)
        _loadMorePlugin.notifyInit(vm)

        viewModelScope.launch {
            _refreshPlugin.state
                .map { it.isLoading }
                .distinctUntilChanged()
                .collect { isLoading ->
                    _state.update {
                        it.copy(isRefreshing = isLoading)
                    }
                }
        }
        viewModelScope.launch {
            _loadMorePlugin.state
                .map { it.isLoading }
                .distinctUntilChanged()
                .collect { isLoading ->
                    _state.update {
                        it.copy(isLoadingMore = isLoading)
                    }
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _refreshPlugin.notifyDestroy()
        _loadMorePlugin.notifyDestroy()
    }
}