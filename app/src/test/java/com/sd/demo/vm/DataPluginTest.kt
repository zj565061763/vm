package com.sd.demo.vm

import android.os.Looper
import androidx.annotation.VisibleForTesting
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.sd.lib.vm.PluginViewModel
import com.sd.lib.vm.plugin.DataPlugin
import com.sd.lib.vm.plugin.DataState
import com.sd.lib.vm.plugin.isInitial
import com.sd.lib.vm.plugin.isSuccess
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll
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
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class DataPluginTest {
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
                assertEquals(0, state.data)
                assertEquals(true, state.isInitial)
                assertEquals(false, state.isLoading)
            }

            awaitItem().let { state ->
                assertEquals(0, state.data)
                assertEquals(true, state.isInitial)
                assertEquals(true, state.isLoading)
            }

            awaitItem().let { state ->
                assertEquals(1, state.data)
                assertEquals(true, state.isSuccess)
                assertEquals(true, state.isLoading)
            }

            awaitItem().let { state ->
                assertEquals(1, state.data)
                assertEquals(true, state.isSuccess)
                assertEquals(false, state.isLoading)
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
                assertEquals(0, state.data)
                assertEquals(true, state.isInitial)
                assertEquals(false, state.isLoading)
            }

            awaitItem().let { state ->
                assertEquals(1, state.data)
                assertEquals(true, state.isSuccess)
                assertEquals(false, state.isLoading)
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
                assertEquals(0, state.data)
                assertEquals(true, state.isInitial)
                assertEquals(false, state.isLoading)
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
                assertEquals(0, state.data)
                assertEquals(true, state.isInitial)
                assertEquals(false, state.isLoading)
            }

            awaitItem().let { state ->
                assertEquals(0, state.data)
                assertEquals(true, state.isInitial)
                assertEquals(true, state.isLoading)
            }

            awaitItem().let { state ->
                assertEquals(1, state.data)
                assertEquals(true, state.isSuccess)
                assertEquals(true, state.isLoading)
            }

            awaitItem().let { state ->
                assertEquals(1, state.data)
                assertEquals(true, state.isSuccess)
                assertEquals(false, state.isLoading)
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun TestScope.testLoad(
    initialActive: Boolean,
    notifyLoading: Boolean,
    ignoreActive: Boolean,
    validate: suspend TurbineTestContext<DataState<Int>>.() -> Unit,
) {
    val vm = DataPluginViewModel().apply { setActive(initialActive) }
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

@VisibleForTesting
private class DataPluginViewModel : PluginViewModel<Unit>() {
    private var _count = 0
    private val _plugin = DataPlugin(_count).register()

    val state = _plugin.state

    fun load(
        notifyLoading: Boolean,
        ignoreActive: Boolean,
    ) {
        _plugin.load(
            notifyLoading = notifyLoading,
            ignoreActive = ignoreActive,
        ) {
            delay(1.seconds)
            _count++
            Result.success(_count)
        }
    }
}