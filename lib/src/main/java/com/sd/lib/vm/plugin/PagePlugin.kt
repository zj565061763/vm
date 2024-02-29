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
     * @param notifyRefreshing 是否通知[PageState.isRefreshing]
     * @param ignoreActive 是否忽略激活状态[FViewModel.isVMActive]
     */
    fun refresh(
        notifyRefreshing: Boolean = true,
        ignoreActive: Boolean = false,
    )

    /**
     * 加载更多数据
     *
     * @param notifyLoadingMore 是否通知[PageState.isLoadingMore]
     * @param ignoreActive 是否忽略激活状态[FViewModel.isVMActive]
     */
    fun loadMore(
        notifyLoadingMore: Boolean = true,
        ignoreActive: Boolean = false,
    )

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
 * @param refreshPage 刷新数据的页码，例如数据源规定页码从1开始，那么此参数就为1
 * @param onLoad 数据加载回调
 */
fun <T> PagePlugin(
    refreshPage: Int = 1,
    onLoad: suspend (page: Int) -> Result<PagePlugin.Data<T>>,
): PagePlugin<T> {
    return PagePluginImpl(
        refreshPage = refreshPage,
        onLoad = onLoad,
    )
}

data class PageState<T>(
    /** 所有页的数据 */
    val data: List<T> = emptyList(),

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

//---------- impl ----------

private class PagePluginImpl<T>(
    override val refreshPage: Int,
    private val onLoad: suspend (page: Int) -> Result<PagePlugin.Data<T>>,
) : ViewModelPlugin(), PagePlugin<T> {

    private val _refreshPlugin = DataPlugin(Unit) {
        val page = refreshPage

        _loadMorePlugin.cancelLoad()
        val result = onLoad(page)
        handleLoadResult(result, page)

        result.map { }
    }

    private val _loadMorePlugin = DataPlugin(Unit) {
        val page = state.value.page + 1

        val result = onLoad(page)
        handleLoadResult(result, page)

        result.map { }
    }

    private val _state = MutableStateFlow(PageState<T>())
    override val state: StateFlow<PageState<T>> = _state.asStateFlow()

    override fun refresh(
        notifyRefreshing: Boolean,
        ignoreActive: Boolean,
    ) {
        _refreshPlugin.load(
            notifyLoading = notifyRefreshing,
            ignoreActive = ignoreActive,
        )
    }

    override fun loadMore(
        notifyLoadingMore: Boolean,
        ignoreActive: Boolean,
    ) {
        with(state.value) {
            if (isRefreshing || isLoadingMore) {
                return
            }
        }

        _loadMorePlugin.load(
            notifyLoading = notifyLoadingMore,
            ignoreActive = ignoreActive,
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