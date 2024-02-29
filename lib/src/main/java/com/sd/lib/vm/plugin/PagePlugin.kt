package com.sd.lib.vm.plugin

import com.sd.lib.vm.AbsViewModelPlugin
import com.sd.lib.vm.FViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class PageState<T>(
    /** 数据 */
    val data: List<T> = emptyList(),

    /** 数据结果 */
    val result: Result<Unit>? = null,

    /** 当前页码 */
    val currentPage: Int = 0,

    /** 是否还有更多数据 */
    val hasMore: Boolean = false,

    /** 是否正在刷新 */
    val isRefreshing: Boolean = false,

    /** 是否正在加载更多 */
    val isLoadingMore: Boolean = false,
)

/**
 * 分页管理
 */
interface PagePlugin<T> : StatePlugin<PageState<T>> {

    /** 刷新数据的页码，例如数据源规定页码从1开始，那么此参数就为1 */
    val refreshPage: Int

    /**
     * 刷新数据
     *
     * @param notifyRefreshing 是否通知[PageState.isRefreshing]
     * @param ignoreActive 是否忽略激活状态[FViewModel.isActiveFlow]
     */
    fun refresh(
        notifyRefreshing: Boolean = true,
        ignoreActive: Boolean = false,
    )

    /**
     * 加载更多数据
     */
    fun loadMore()

    data class Data<T>(
        /** 数据 */
        val data: List<T>,

        /** 本页实际加载到的数据个数 */
        val pageSize: Int,

        /** 是否还有更多数据 */
        val hasMore: Boolean = false,
    )
}

/**
 * [PagePlugin]
 */
fun <T> PagePlugin(
    /** 状态管理 */
    stater: Stater<PageState<T>> = Stater(PageState()),
    /** 刷新数据的页码，例如数据源规定页码从1开始，那么此参数就为1 */
    refreshPage: Int = 1,
    /** 加载回调 */
    onLoad: suspend (page: Int) -> Result<PagePlugin.Data<T>>,
): PagePlugin<T> {
    return PagePluginImpl(
        stater = stater,
        refreshPage = refreshPage,
        onLoad = onLoad,
    )
}

private class PagePluginImpl<T>(
    /** 状态管理 */
    private val stater: Stater<PageState<T>>,
    /** 刷新数据的页码，例如数据源规定页码从1开始，那么此参数就为1 */
    override val refreshPage: Int,
    /** 加载回调 */
    private val onLoad: suspend (page: Int) -> Result<PagePlugin.Data<T>>,
) : AbsViewModelPlugin(), PagePlugin<T> {

    /** 刷新 */
    private val _refreshPlugin = DataPlugin {
        // 刷新之前，取消加载更多
        _loadMorePlugin.cancelLoad()
        val page = refreshPage
        onLoad(page).also { result ->
            handleLoadResult(result, page)
        }.map { }
    }

    /** 加载更多 */
    private val _loadMorePlugin = DataPlugin {
        val page = state.value.currentPage + 1
        onLoad(page).also { result ->
            handleLoadResult(result, page)
        }.map { }
    }

    override val state: StateFlow<PageState<T>>
        get() = stater.state

    override fun refresh(
        notifyRefreshing: Boolean,
        ignoreActive: Boolean,
    ) {
        _refreshPlugin.load(
            notifyLoading = notifyRefreshing,
            ignoreActive = ignoreActive,
        )
    }

    override fun loadMore() {
        with(state.value) {
            if (isRefreshing || isLoadingMore) {
                return
            }
        }

        _loadMorePlugin.load(
            notifyLoading = true,
            ignoreActive = false,
        )
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
                if (data.pageSize > 0) {
                    page
                } else {
                    page - 1
                }
            }

            stater.update {
                it.copy(
                    result = Result.success(Unit),
                    data = data.data,
                    currentPage = newPage,
                    hasMore = data.hasMore,
                )
            }
        }

        result.onFailure { throwable ->
            if (state.value.data.isEmpty()) {
                stater.update {
                    it.copy(result = Result.failure(throwable))
                }
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
                    stater.update {
                        it.copy(isRefreshing = isLoading)
                    }
                }
        }
        viewModelScope.launch {
            _loadMorePlugin.state
                .map { it.isLoading }
                .distinctUntilChanged()
                .collect { isLoading ->
                    stater.update {
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