package com.sd.demo.vm

import androidx.lifecycle.ViewModel
import com.sd.lib.vm.FViewModel
import com.sd.lib.vm.IgnoreActiveIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test clear`() {
        val vm = TestViewModel()
        assertEquals(false, vm.isVMDestroyed)

        vm.clearViewModel()
        assertEquals(true, vm.isVMDestroyed)
        assertEquals("onDestroy()", vm.callbackString)
    }

    @Test
    fun `test active`() = runTest {
        val vm = TestViewModel()
        assertEquals(true, vm.isVMActive)
        advanceUntilIdle()
        assertEquals("", vm.callbackString)

        vm.setActive(false)
        assertEquals(false, vm.isVMActive)
        advanceUntilIdle()
        assertEquals("onInActive()", vm.callbackString)

        vm.setActive(true)
        assertEquals(true, vm.isVMActive)
        advanceUntilIdle()
        assertEquals("onActive()", vm.callbackString)
    }

    @Test
    fun `test intent`() = runTest {
        val vm = TestIntentViewModel().apply { setActive(true) }
        assertEquals(true, vm.isVMActive)

        vm.dispatch(TestIntentViewModel.Intent.ActiveContent)
        vm.dispatch(TestIntentViewModel.Intent.ActiveContent)
        advanceUntilIdle()
        assertEquals(2, vm.count)

        vm.clearViewModel()

        vm.dispatch(TestIntentViewModel.Intent.ActiveContent)
        vm.dispatch(TestIntentViewModel.Intent.ActiveContent)
        advanceUntilIdle()
        assertEquals(2, vm.count)
    }

    @Test
    fun `test intent inactive`() = runTest {
        val vm = TestIntentViewModel().apply { setActive(false) }
        assertEquals(false, vm.isVMActive)

        vm.dispatch(TestIntentViewModel.Intent.ActiveContent)
        vm.dispatch(TestIntentViewModel.Intent.ActiveContent)
        advanceUntilIdle()

        assertEquals(0, vm.count)
    }

    @Test
    fun `test intent inactive ignore`() = runTest {
        val vm = TestIntentViewModel().apply { setActive(false) }
        assertEquals(false, vm.isVMActive)

        vm.dispatch(TestIntentViewModel.Intent.IgnoreActiveContent)
        vm.dispatch(TestIntentViewModel.Intent.IgnoreActiveContent)
        advanceUntilIdle()
        assertEquals(2, vm.count)

        vm.clearViewModel()

        vm.dispatch(TestIntentViewModel.Intent.IgnoreActiveContent)
        vm.dispatch(TestIntentViewModel.Intent.IgnoreActiveContent)
        advanceUntilIdle()
        assertEquals(2, vm.count)
    }
}

private class TestViewModel : FViewModel<Unit>() {
    var callbackString = ""

    override fun onActive() {
        super.onActive()
        callbackString = "onActive()"
    }

    override fun onInActive() {
        super.onInActive()
        callbackString = "onInActive()"
    }

    override fun onDestroy() {
        super.onDestroy()
        callbackString = "onDestroy()"
    }
}

private class TestIntentViewModel : FViewModel<TestIntentViewModel.Intent>() {
    var count = 0
        private set

    override suspend fun handleIntent(intent: Intent) {
        super.handleIntent(intent)
        when (intent) {
            is Intent.ActiveContent -> count++
            is Intent.IgnoreActiveContent -> count++
        }
    }

    sealed interface Intent {
        data object ActiveContent : Intent
        data object IgnoreActiveContent : Intent, IgnoreActiveIntent
    }
}

fun ViewModel.clearViewModel() {
    ViewModel::class.java.getDeclaredMethod("clear").apply {
        this.isAccessible = true
        this.invoke(this@clearViewModel)
    }
}