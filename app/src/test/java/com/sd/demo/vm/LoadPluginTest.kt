package com.sd.demo.vm

import android.os.Looper
import androidx.annotation.VisibleForTesting
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.sd.lib.vm.PluginViewModel
import com.sd.lib.vm.plugin.LoadPlugin
import com.sd.lib.vm.plugin.LoadState
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
import kotlin.time.Duration.Companion.seconds

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
    }

    @Test
    fun `test load`() = runTest {
        testLoad(
            initialActive = true,
            notifyLoading = true,
            ignoreActive = false
        ) { vm ->
            awaitItem().let { state ->
                assertEquals(false, state.isLoading)
            }

            awaitItem().let { state ->
                assertEquals(true, state.isLoading)
            }

            awaitItem().let { state ->
                assertEquals(false, state.isLoading)
            }

            assertEquals(1, vm.count)
        }
    }

    @Test
    fun `test load notifyLoading false`() = runTest {
        testLoad(
            initialActive = true,
            notifyLoading = false,
            ignoreActive = false
        ) { vm ->
            awaitItem().let { state ->
                assertEquals(false, state.isLoading)
            }

            assertEquals(1, vm.count)
        }
    }

    @Test
    fun `test load inactive`() = runTest {
        testLoad(
            initialActive = false,
            notifyLoading = true,
            ignoreActive = false
        ) { vm ->
            awaitItem().let { state ->
                assertEquals(false, state.isLoading)
            }

            assertEquals(0, vm.count)
        }
    }

    @Test
    fun `test load inactive ignore`() = runTest {
        testLoad(
            initialActive = false,
            notifyLoading = true,
            ignoreActive = true
        ) { vm ->
            awaitItem().let { state ->
                assertEquals(false, state.isLoading)
            }

            awaitItem().let { state ->
                assertEquals(true, state.isLoading)
            }

            awaitItem().let { state ->
                assertEquals(false, state.isLoading)
            }

            assertEquals(1, vm.count)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun TestScope.testLoad(
    initialActive: Boolean,
    notifyLoading: Boolean,
    ignoreActive: Boolean,
    validate: suspend TurbineTestContext<LoadState>.(LoadPluginViewModel) -> Unit,
) {
    val vm = LoadPluginViewModel().apply { setActive(initialActive) }
    assertEquals(initialActive, vm.isVMActive)

    vm.state.test {
        vm.load(
            notifyLoading = notifyLoading,
            ignoreActive = ignoreActive,
        )
        advanceUntilIdle()
        validate(vm)
    }
}

@VisibleForTesting
private class LoadPluginViewModel : PluginViewModel<Unit>() {
    private val _plugin = LoadPlugin().register()

    var count = 0
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
            count++
        }
    }
}