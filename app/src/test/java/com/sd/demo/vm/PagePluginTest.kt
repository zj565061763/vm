package com.sd.demo.vm

import android.os.Looper
import androidx.annotation.VisibleForTesting
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.sd.lib.vm.PluginViewModel
import com.sd.lib.vm.plugin.PagePlugin
import com.sd.lib.vm.plugin.PageState
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
            notifyRefreshing = true,
            ignoreActive = false
        ) {
            awaitItem().let { state ->
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
                assertEquals(null, state.result)
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(0, state.currentPage)
                assertEquals(false, state.hasMore)
            }

            awaitItem().let { state ->
                assertEquals(true, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
                assertEquals(null, state.result)
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(0, state.currentPage)
                assertEquals(false, state.hasMore)
            }

            awaitItem().let { state ->
                assertEquals(true, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
                assertEquals(true, state.result?.isSuccess)
                assertEquals(PagePluginViewModel.pageList(), state.data)
                assertEquals(1, state.currentPage)
                assertEquals(true, state.hasMore)
            }

            awaitItem().let { state ->
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
                assertEquals(true, state.result?.isSuccess)
                assertEquals(PagePluginViewModel.pageList(), state.data)
                assertEquals(1, state.currentPage)
                assertEquals(true, state.hasMore)
            }
        }
    }

    @Test
    fun `test refresh notifyRefreshing false`() = runTest {
        testRefresh(
            initialActive = true,
            notifyRefreshing = false,
            ignoreActive = false
        ) {
            awaitItem().let { state ->
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
                assertEquals(null, state.result)
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(0, state.currentPage)
                assertEquals(false, state.hasMore)
            }

            awaitItem().let { state ->
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
                assertEquals(true, state.result?.isSuccess)
                assertEquals(PagePluginViewModel.pageList(), state.data)
                assertEquals(1, state.currentPage)
                assertEquals(true, state.hasMore)
            }
        }
    }

    @Test
    fun `test refresh inactive`() = runTest {
        testRefresh(
            initialActive = false,
            notifyRefreshing = true,
            ignoreActive = false
        ) {
            awaitItem().let { state ->
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
                assertEquals(null, state.result)
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(0, state.currentPage)
                assertEquals(false, state.hasMore)
            }
        }
    }

    @Test
    fun `test refresh inactive ignore`() = runTest {
        testRefresh(
            initialActive = false,
            notifyRefreshing = true,
            ignoreActive = true
        ) {
            awaitItem().let { state ->
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
                assertEquals(null, state.result)
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(0, state.currentPage)
                assertEquals(false, state.hasMore)
            }

            awaitItem().let { state ->
                assertEquals(true, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
                assertEquals(null, state.result)
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(0, state.currentPage)
                assertEquals(false, state.hasMore)
            }

            awaitItem().let { state ->
                assertEquals(true, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
                assertEquals(true, state.result?.isSuccess)
                assertEquals(PagePluginViewModel.pageList(), state.data)
                assertEquals(1, state.currentPage)
                assertEquals(true, state.hasMore)
            }

            awaitItem().let { state ->
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
                assertEquals(true, state.result?.isSuccess)
                assertEquals(PagePluginViewModel.pageList(), state.data)
                assertEquals(1, state.currentPage)
                assertEquals(true, state.hasMore)
            }
        }
    }

    @Test
    fun `test load more`() = runTest {
        testLoadMore(
            initialActive = true,
        ) {
            awaitItem().let { state ->
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
                assertEquals(null, state.result)
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(0, state.currentPage)
                assertEquals(false, state.hasMore)
            }

            awaitItem().let { state ->
                assertEquals(false, state.isRefreshing)
                assertEquals(true, state.isLoadingMore)
                assertEquals(null, state.result)
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(0, state.currentPage)
                assertEquals(false, state.hasMore)
            }

            awaitItem().let { state ->
                assertEquals(false, state.isRefreshing)
                assertEquals(true, state.isLoadingMore)
                assertEquals(true, state.result?.isSuccess)
                assertEquals(PagePluginViewModel.pageList(), state.data)
                assertEquals(1, state.currentPage)
                assertEquals(true, state.hasMore)
            }

            awaitItem().let { state ->
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
                assertEquals(true, state.result?.isSuccess)
                assertEquals(PagePluginViewModel.pageList(), state.data)
                assertEquals(1, state.currentPage)
                assertEquals(true, state.hasMore)
            }
        }
    }

    @Test
    fun `test load more inactive`() = runTest {
        testLoadMore(
            initialActive = false,
        ) {
            awaitItem().let { state ->
                assertEquals(false, state.isRefreshing)
                assertEquals(false, state.isLoadingMore)
                assertEquals(null, state.result)
                assertEquals(emptyList<Int>(), state.data)
                assertEquals(0, state.currentPage)
                assertEquals(false, state.hasMore)
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun TestScope.testRefresh(
    initialActive: Boolean,
    notifyRefreshing: Boolean,
    ignoreActive: Boolean,
    validate: suspend TurbineTestContext<PageState<Int>>.() -> Unit,
) {
    val vm = PagePluginViewModel().apply { setActive(initialActive) }
    assertEquals(initialActive, vm.isVMActive)

    vm.dataPlugin.state.test {
        vm.dataPlugin.refresh(
            notifyRefreshing = notifyRefreshing,
            ignoreActive = ignoreActive,
        )
        advanceUntilIdle()
        validate()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun TestScope.testLoadMore(
    initialActive: Boolean,
    validate: suspend TurbineTestContext<PageState<Int>>.() -> Unit,
) {
    val vm = PagePluginViewModel().apply { setActive(initialActive) }
    assertEquals(initialActive, vm.isVMActive)

    vm.dataPlugin.state.test {
        vm.dataPlugin.loadMore()
        advanceUntilIdle()
        validate()
    }
}

@VisibleForTesting
private class PagePluginViewModel : PluginViewModel<Unit>() {
    val dataPlugin = PagePlugin { loadData(it) }.register()

    private val _list = mutableListOf<Int>()

    override suspend fun handleIntent(intent: Unit) {}

    private suspend fun loadData(page: Int): Result<PagePlugin.Data<Int>> {
        delay(1000)

        val list = pageList()
        if (page == dataPlugin.refreshPage) {
            _list.clear()
            _list.addAll(list)
        } else {
            _list.addAll(list)
        }

        return Result.success(
            PagePlugin.Data(
                data = _list.toList(),
                hasMore = true,
                pageSize = list.size,
            )
        )
    }

    companion object {
        fun pageList(): List<Int> {
            return Array(10) { it + 1 }.toList()
        }
    }
}