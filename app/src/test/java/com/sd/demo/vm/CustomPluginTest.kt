package com.sd.demo.vm

import android.os.Looper
import com.sd.lib.vm.FViewModel
import com.sd.lib.vm.plugin.RealVMPlugin
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll
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
        unmockkAll()
    }

    @Test
    fun `test custom plugin`() {
        val vm = CustomPluginViewModel()
        assertEquals(1, vm.customPlugin.count)

        vm.tryClear()
        assertEquals(2, vm.customPlugin.count)
    }
}

private class CustomPluginViewModel : FViewModel<Unit>() {
    val customPlugin = plugin { CustomPlugin() }
}

private class CustomPlugin : RealVMPlugin() {
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