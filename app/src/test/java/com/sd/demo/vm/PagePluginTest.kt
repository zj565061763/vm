package com.sd.demo.vm

import android.os.Looper
import androidx.annotation.VisibleForTesting
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.sd.lib.vm.PluginViewModel
import com.sd.lib.vm.plugin.PagePlugin
import com.sd.lib.vm.plugin.PageState
import com.sd.lib.vm.plugin.isInitial
import com.sd.lib.vm.plugin.isSuccess
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PagePluginTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())

        val looper = mockkClass(Looper::class)
        mockkStatic(Looper::class)
        every { Looper.myLooper() } returns looper
        every { Looper.getMainLooper() } returns looper
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test refresh`() = runTest {
        testRefresh(
            initialActive = true,
            notifyLoading = true,
            ignoreActive = false
        ) {
            awaitItem().let { state ->
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(null, state.loadPage)
                assertEquals(true, state.isInitial)
                assertEquals(null, state.hasMore)
                assertEquals(null, state.isRefreshResult)
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
            }

            awaitItem().let { state ->
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(null, state.loadPage)
                assertEquals(true, state.isInitial)
                assertEquals(null, state.hasMore)
                assertEquals(null, state.isRefreshResult)
                assertEquals(true, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
            }

            awaitItem().let { state ->
                assertEquals(PagePluginViewModel.pageList(), state.data)
                assertEquals(1, state.loadPage)
                assertEquals(true, state.isSuccess)
                assertEquals(true, state.hasMore)
                assertEquals(true, state.isRefreshResult)
                assertEquals(true, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
            }

            awaitItem().let { state ->
                assertEquals(PagePluginViewModel.pageList(), state.data)
                assertEquals(1, state.loadPage)
                assertEquals(true, state.isSuccess)
                assertEquals(true, state.hasMore)
                assertEquals(true, state.isRefreshResult)
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
            }
        }
    }

    @Test
    fun `test refresh notifyRefreshing false`() = runTest {
        testRefresh(
            initialActive = true,
            notifyLoading = false,
            ignoreActive = false
        ) {
            awaitItem().let { state ->
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(null, state.loadPage)
                assertEquals(true, state.isInitial)
                assertEquals(null, state.isRefreshResult)
                assertEquals(null, state.hasMore)
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
            }

            awaitItem().let { state ->
                assertEquals(PagePluginViewModel.pageList(), state.data)
                assertEquals(1, state.loadPage)
                assertEquals(true, state.isSuccess)
                assertEquals(true, state.isRefreshResult)
                assertEquals(true, state.hasMore)
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
            }
        }
    }

    @Test
    fun `test refresh inactive`() = runTest {
        testRefresh(
            initialActive = false,
            notifyLoading = true,
            ignoreActive = false
        ) {
            awaitItem().let { state ->
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(null, state.loadPage)
                assertEquals(true, state.isInitial)
                assertEquals(null, state.isRefreshResult)
                assertEquals(null, state.hasMore)
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
            }
        }
    }

    @Test
    fun `test refresh inactive ignore`() = runTest {
        testRefresh(
            initialActive = false,
            notifyLoading = true,
            ignoreActive = true
        ) {
            awaitItem().let { state ->
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(null, state.loadPage)
                assertEquals(true, state.isInitial)
                assertEquals(null, state.isRefreshResult)
                assertEquals(null, state.hasMore)
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
            }

            awaitItem().let { state ->
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(null, state.loadPage)
                assertEquals(true, state.isInitial)
                assertEquals(null, state.isRefreshResult)
                assertEquals(null, state.hasMore)
                assertEquals(true, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
            }

            awaitItem().let { state ->
                assertEquals(PagePluginViewModel.pageList(), state.data)
                assertEquals(1, state.loadPage)
                assertEquals(true, state.isSuccess)
                assertEquals(true, state.hasMore)
                assertEquals(true, state.isRefreshResult)
                assertEquals(true, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
            }

            awaitItem().let { state ->
                assertEquals(PagePluginViewModel.pageList(), state.data)
                assertEquals(1, state.loadPage)
                assertEquals(true, state.isSuccess)
                assertEquals(true, state.hasMore)
                assertEquals(true, state.isRefreshResult)
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
            }
        }
    }

    @Test
    fun `test load more`() = runTest {
        testLoadMore(
            initialActive = true,
            notifyLoading = true,
            ignoreActive = false,
        ) {
            awaitItem().let { state ->
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(null, state.loadPage)
                assertEquals(true, state.isInitial)
                assertEquals(null, state.hasMore)
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
            }

            awaitItem().let { state ->
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(null, state.loadPage)
                assertEquals(true, state.isInitial)
                assertEquals(null, state.hasMore)
                assertEquals(false, state.isRefreshing)
                assertEquals(true, state.isLoadingMore)
            }

            awaitItem().let { state ->
                assertEquals(PagePluginViewModel.pageList(), state.data)
                assertEquals(1, state.loadPage)
                assertEquals(true, state.isSuccess)
                assertEquals(true, state.hasMore)
                assertEquals(false, state.isRefreshing)
                assertEquals(true, state.isLoadingMore)
            }

            awaitItem().let { state ->
                assertEquals(PagePluginViewModel.pageList(), state.data)
                assertEquals(1, state.loadPage)
                assertEquals(true, state.isSuccess)
                assertEquals(true, state.hasMore)
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
            }
        }
    }

    @Test
    fun `test load more notifyLoadingMore false`() = runTest {
        testLoadMore(
            initialActive = true,
            notifyLoading = false,
            ignoreActive = false
        ) {
            awaitItem().let { state ->
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(null, state.loadPage)
                assertEquals(true, state.isInitial)
                assertEquals(null, state.hasMore)
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
            }

            awaitItem().let { state ->
                assertEquals(PagePluginViewModel.pageList(), state.data)
                assertEquals(1, state.loadPage)
                assertEquals(true, state.isSuccess)
                assertEquals(true, state.hasMore)
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
            }
        }
    }

    @Test
    fun `test load more inactive`() = runTest {
        testLoadMore(
            initialActive = false,
            notifyLoading = true,
            ignoreActive = false,
        ) {
            awaitItem().let { state ->
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(null, state.loadPage)
                assertEquals(true, state.isInitial)
                assertEquals(null, state.hasMore)
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
            }
        }
    }

    @Test
    fun `test load more inactive ignore`() = runTest {
        testLoadMore(
            initialActive = false,
            notifyLoading = true,
            ignoreActive = true,
        ) {
            awaitItem().let { state ->
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(null, state.loadPage)
                assertEquals(true, state.isInitial)
                assertEquals(null, state.hasMore)
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
            }

            awaitItem().let { state ->
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(null, state.loadPage)
                assertEquals(true, state.isInitial)
                assertEquals(null, state.hasMore)
                assertEquals(false, state.isRefreshing)
                assertEquals(true, state.isLoadingMore)
            }

            awaitItem().let { state ->
                assertEquals(PagePluginViewModel.pageList(), state.data)
                assertEquals(1, state.loadPage)
                assertEquals(true, state.isSuccess)
                assertEquals(true, state.hasMore)
                assertEquals(false, state.isRefreshing)
                assertEquals(true, state.isLoadingMore)
            }

            awaitItem().let { state ->
                assertEquals(PagePluginViewModel.pageList(), state.data)
                assertEquals(1, state.loadPage)
                assertEquals(true, state.isSuccess)
                assertEquals(true, state.hasMore)
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun TestScope.testRefresh(
    initialActive: Boolean,
    notifyLoading: Boolean,
    ignoreActive: Boolean,
    validate: suspend TurbineTestContext<PageState<Int>>.() -> Unit,
) {
    val vm = PagePluginViewModel().apply { setActive(initialActive) }
    assertEquals(initialActive, vm.isVMActive)

    vm.plugin.state.test {
        vm.plugin.refresh(
            notifyLoading = notifyLoading,
            ignoreActive = ignoreActive,
        )
        advanceUntilIdle()
        validate()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun TestScope.testLoadMore(
    initialActive: Boolean,
    notifyLoading: Boolean,
    ignoreActive: Boolean,
    validate: suspend TurbineTestContext<PageState<Int>>.() -> Unit,
) {
    val vm = PagePluginViewModel().apply { setActive(initialActive) }
    assertEquals(initialActive, vm.isVMActive)

    vm.plugin.state.test {
        vm.plugin.loadMore(
            notifyLoading = notifyLoading,
            ignoreActive = ignoreActive,
        )
        advanceUntilIdle()
        validate()
    }
}

@VisibleForTesting
private class PagePluginViewModel : PluginViewModel<Unit>() {
    private val _list = mutableListOf<Int>()

    val plugin = PagePlugin { loadData(it, isRefresh) }.register()

    private suspend fun loadData(
        page: Int,
        isRefresh: Boolean,
    ): PagePlugin.LoadResult<Int> {
        delay(1000)

        val list = pageList()
        if (isRefresh) {
            _list.clear()
            _list.addAll(list)
        } else {
            _list.addAll(list)
        }

        return PagePlugin.resultSuccess(
            data = _list.toList(),
            pageSize = list.size,
            hasMore = true,
        )
    }

    companion object {
        fun pageList(): List<Int> {
            return Array(10) { it + 1 }.toList()
        }
    }
}