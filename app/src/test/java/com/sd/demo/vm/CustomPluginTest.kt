package com.sd.demo.vm

import android.os.Looper
import androidx.annotation.VisibleForTesting
import com.sd.lib.vm.PluginViewModel
import com.sd.lib.vm.plugin.ViewModelPlugin
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomPluginTest {
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
    fun `test custom plugin`() {
        val vm = CustomPluginViewModel()
        assertEquals(1, vm.customPlugin.count)

        vm.clearViewModel()
        assertEquals(2, vm.customPlugin.count)
    }
}

@VisibleForTesting
private class CustomPluginViewModel : PluginViewModel<Unit>() {
    val customPlugin = CustomPlugin().register()

    override suspend fun handleIntent(intent: Unit) {}
}

private class CustomPlugin : ViewModelPlugin() {
    var count = 0
        private set

    override fun onInit() {
        super.onInit()
        count++
    }

    override fun onDestroy() {
        super.onDestroy()
        count++
    }
}