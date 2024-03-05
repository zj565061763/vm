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
interface PagePlugin<T> : StatePlugin<StateFlow<PageState<T>>> {

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
     * 取消刷新数据
     */
    fun cancelRefresh()

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

    /**
     * 取消加载更多
     */
    fun cancelLoadMore()

    interface LoadScope<T> {
        /** 当前数据状态 */
        val currentState: PageState<T>

        /** true-[refresh]，false-[loadMore] */
        val isRefresh: Boolean
    }

    sealed interface LoadResult<T> {
        data class Success<T>(
            /** 总数据，null-数据不变 */
            val data: List<T>?,

            /** 本页实际加载到的数据个数 */
            val pageSize: Int,

            /** 是否还有更多数据 */
            val hasMore: Boolean,
        ) : LoadResult<T>

        data class Failure<T>(
            /** 异常信息 */
            val exception: Throwable,

            /** 总数据，null-数据不变 */
            val data: List<T>? = null,
        ) : LoadResult<T>

        data class None<T>(
            /** 总数据，null-数据不变 */
            val data: List<T>? = null,
        ) : LoadResult<T>
    }

    companion object {
        /**
         * 加载成功
         *
         * @param data 总数据，null-数据不变
         * @param pageSize 本页实际加载到的数据个数
         * @param hasMore 是否还有更多数据
         */
        fun <T> resultSuccess(
            data: List<T>?,
            pageSize: Int,
            hasMore: Boolean,
        ): LoadResult<T> {
            return LoadResult.Success(
                data = data,
                pageSize = pageSize,
                hasMore = hasMore,
            )
        }

        /**
         * 加载失败
         *
         * @param exception 异常信息
         * @param data 总数据，null-数据不变
         */
        fun <T> resultFailure(
            exception: Throwable,
            data: List<T>? = null,
        ): LoadResult<T> {
            return LoadResult.Failure(
                exception = exception,
                data = data,
            )
        }
    }
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
    canLoad: suspend PagePlugin.LoadScope<T>.(page: Int) -> Boolean = {
        if (isRefresh) {
            true
        } else {
            currentState.run { !isRefreshing && !isLoadingMore }
        }
    },
    onLoad: suspend PagePlugin.LoadScope<T>.(page: Int) -> PagePlugin.LoadResult<T>,
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
    /** 总数据 */
    val data: List<T> = emptyList(),

    /** 最后一次加载的页码 */
    val loadPage: Int? = null,

    /** 最后一次加载的结果，true-刷新结果，false-加载更多结果 */
    val loadResult: Result<Boolean>? = null,

    /** 是否还有更多数据 */
    val hasMore: Boolean? = null,

    /** 是否正在刷新 */
    val isRefreshing: Boolean = false,

    /** 是否正在加载更多 */
    val isLoadingMore: Boolean = false,
)

/** 是否初始状态 */
val PageState<*>.isInitial: Boolean get() = loadResult == null

/** 是否成功状态 */
val PageState<*>.isSuccess: Boolean get() = loadResult?.isSuccess == true

/** 是否失败状态 */
val PageState<*>.isFailure: Boolean get() = loadResult?.isFailure == true

/**
 * 初始状态
 */
inline fun <T> PageState<T>.onInitial(action: PageState<T>.() -> Unit): PageState<T> {
    if (loadResult == null) action()
    return this
}

/**
 * 成功状态
 */
inline fun <T> PageState<T>.onSuccess(action: PageState<T>.(Boolean) -> Unit): PageState<T> {
    loadResult?.onSuccess { action(it) }
    return this
}

/**
 * 失败状态
 */
inline fun <T> PageState<T>.onFailure(action: PageState<T>.(exception: Throwable) -> Unit): PageState<T> {
    loadResult?.onFailure { action(it) }
    return this
}

/**
 * 加载成功，并且总数据为空
 */
inline fun <T> PageState<T>.onViewSuccessEmpty(action: PageState<T>.(Boolean) -> Unit): PageState<T> {
    onSuccess {
        if (data.isEmpty()) action(it)
    }
    return this
}

/**
 * 加载失败，并且总数据为空
 */
inline fun <T> PageState<T>.onViewFailureEmpty(action: PageState<T>.(exception: Throwable) -> Unit): PageState<T> {
    onFailure {
        if (data.isEmpty()) action(it)
    }
    return this
}

//---------- impl ----------

private class PagePluginImpl<T>(
    initial: List<T>,
    private val refreshPage: Int,
    private val canLoad: suspend PagePlugin.LoadScope<T>.(page: Int) -> Boolean,
    private val onLoad: suspend PagePlugin.LoadScope<T>.(page: Int) -> PagePlugin.LoadResult<T>,
) : ViewModelPlugin(), PagePlugin<T> {

    private var _currentPage = refreshPage - 1

    private val loadMorePage: Int
        get() = if (state.value.data.isEmpty()) refreshPage else _currentPage + 1

    private val _refreshPlugin = LoadPlugin()
    private val _loadMorePlugin = LoadPlugin()

    private val _state = MutableStateFlow(PageState(data = initial))

    override val state: StateFlow<PageState<T>> = _state.asStateFlow()

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
                    isRefresh = true,
                    canLoad = canLoad,
                )
            },
        ) {
            // 刷新之前取消加载更多
            _loadMorePlugin.cancelLoad()
            loadData(
                page = refreshPage,
                isRefresh = true,
            )
        }
    }

    override fun cancelRefresh() {
        _refreshPlugin.cancelLoad()
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
                    page = loadMorePage,
                    isRefresh = false,
                    canLoad = canLoad,
                )
            },
        ) {
            loadData(
                page = loadMorePage,
                isRefresh = false,
            )
        }
    }

    override fun cancelLoadMore() {
        _loadMorePlugin.cancelLoad()
    }

    private suspend fun canLoadData(
        page: Int,
        isRefresh: Boolean,
        canLoad: (suspend PagePlugin.LoadScope<T>.(page: Int) -> Boolean)?,
    ): Boolean {
        val canLoadBlock = canLoad ?: this@PagePluginImpl.canLoad
        return with(newLoadScope(isRefresh)) { canLoadBlock(page) }
    }

    private suspend fun loadData(page: Int, isRefresh: Boolean) {
        val result = with(newLoadScope(isRefresh)) { onLoad(page) }
        handleLoadResult(
            result = result,
            page = page,
            isRefresh = isRefresh,
        )
    }

    private fun handleLoadResult(
        result: PagePlugin.LoadResult<T>,
        page: Int,
        isRefresh: Boolean,
    ) {
        when (result) {
            is PagePlugin.LoadResult.Success<T> -> {
                _currentPage = if (page == refreshPage) {
                    // refresh
                    refreshPage
                } else {
                    // loadMore
                    if (result.pageSize > 0) page else page - 1
                }

                _state.update {
                    it.copy(
                        data = result.data ?: it.data,
                        loadPage = page,
                        loadResult = Result.success(isRefresh),
                        hasMore = result.hasMore,
                    )
                }
            }

            is PagePlugin.LoadResult.Failure<T> -> {
                _state.update {
                    it.copy(
                        data = result.data ?: it.data,
                        loadResult = Result.failure(result.exception),
                    )
                }
            }

            is PagePlugin.LoadResult.None<T> -> {
                _state.update {
                    it.copy(data = result.data ?: it.data)
                }
            }
        }
    }

    private fun newLoadScope(isRefresh: Boolean): PagePlugin.LoadScope<T> {
        return object : PagePlugin.LoadScope<T> {
            override val currentState: PageState<T> get() = state.value
            override val isRefresh: Boolean get() = isRefresh
        }
    }

    override fun onInit() {
        super.onInit()
        _refreshPlugin.register()
        _loadMorePlugin.register()

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
}