package com.sd.demo.vm

import android.os.Looper
import androidx.lifecycle.viewModelScope
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.sd.lib.vm.FViewModel
import com.sd.lib.vm.plugin.LoadPlugin
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
class LoadPluginTest {
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
        unmockkAll()
    }

    @Test
    fun `test load`() = runTest {
        testLoad(
            initialActive = true,
            notifyLoading = true,
            ignoreActive = false
        ) {
            awaitItem().let { state ->
                assertEquals(false, state.isLoading)
                assertEquals(0, state.count)
            }

            awaitItem().let { state ->
                assertEquals(true, state.isLoading)
                assertEquals(0, state.count)
            }

            awaitItem().let { state ->
                assertEquals(true, state.isLoading)
                assertEquals(1, state.count)
            }

            awaitItem().let { state ->
                assertEquals(false, state.isLoading)
                assertEquals(1, state.count)
            }
        }
    }

    @Test
    fun `test load notifyLoading false`() = runTest {
        testLoad(
            initialActive = true,
            notifyLoading = false,
            ignoreActive = false
        ) {
            awaitItem().let { state ->
                assertEquals(false, state.isLoading)
                assertEquals(0, state.count)
            }

            awaitItem().let { state ->
                assertEquals(false, state.isLoading)
                assertEquals(1, state.count)
            }
        }
    }

    @Test
    fun `test load inactive`() = runTest {
        testLoad(
            initialActive = false,
            notifyLoading = true,
            ignoreActive = false
        ) {
            awaitItem().let { state ->
                assertEquals(false, state.isLoading)
                assertEquals(0, state.count)
            }
        }
    }

    @Test
    fun `test load inactive ignore`() = runTest {
        testLoad(
            initialActive = false,
            notifyLoading = true,
            ignoreActive = true
        ) {
            awaitItem().let { state ->
                assertEquals(false, state.isLoading)
                assertEquals(0, state.count)
            }

            awaitItem().let { state ->
                assertEquals(true, state.isLoading)
                assertEquals(0, state.count)
            }

            awaitItem().let { state ->
                assertEquals(true, state.isLoading)
                assertEquals(1, state.count)
            }

            awaitItem().let { state ->
                assertEquals(false, state.isLoading)
                assertEquals(1, state.count)
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun TestScope.testLoad(
    initialActive: Boolean,
    notifyLoading: Boolean,
    ignoreActive: Boolean,
    validate: suspend TurbineTestContext<LoadPluginViewModel.State>.() -> Unit,
) {
    val vm = LoadPluginViewModel().apply { setActive(initialActive) }
    assertEquals(initialActive, vm.isVMActive)

    vm.state.test {
        vm.load(
            notifyLoading = notifyLoading,
            ignoreActive = ignoreActive,
        )
        advanceUntilIdle()
        validate()
    }
}

private class LoadPluginViewModel : FViewModel<Unit>() {
    private val _plugin = plugin { LoadPlugin() }

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    fun load(
        notifyLoading: Boolean,
        ignoreActive: Boolean,
    ) {
        _plugin.load(
            notifyLoading = notifyLoading,
            ignoreActive = ignoreActive,
        ) {
            delay(1_000)
            _state.update {
                it.copy(count = it.count + 1)
            }
        }
    }

    init {
        viewModelScope.launch {
            _plugin.isLoading.collect { isLoading ->
                _state.update {
                    it.copy(isLoading = isLoading)
                }
            }
        }
    }

    data class State(
        val count: Int = 0,
        val isLoading: Boolean = false
    )
}