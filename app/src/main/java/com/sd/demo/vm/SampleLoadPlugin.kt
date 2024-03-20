package com.sd.demo.vm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sd.lib.vm.PluginViewModel
import com.sd.lib.vm.plugin.LoadPlugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class SampleLoadPlugin : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface {
                Content()
            }
        }
    }
}

@Composable
private fun Content(
    modifier: Modifier = Modifier,
    vm: MyLoadViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            modifier = Modifier.widthIn(100.dp),
            onClick = {
                // 加载
                vm.load()
            },
        ) {
            Text(text = "load")
        }

        // 显示加载数据
        Text(text = state.content)

        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                color = LocalContentColor.current,
                strokeWidth = 1.dp,
            )
        }
    }
}

class MyLoadViewModel : PluginViewModel<Unit>() {
    private val _plugin = LoadPlugin().register()

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    fun load() {
        _plugin.load {
            val uuid = UUID.randomUUID().toString()
            logMsg { "load start $uuid" }

            try {
                // 模拟加载数据
                delay(2_000)
            } catch (e: CancellationException) {
                logMsg { "load cancel $uuid" }
                throw e
            }

            _state.update { it.copy(content = uuid) }
            logMsg { "load success $uuid" }
        }
    }

    fun cancelLoad() {
        _plugin.cancelLoad()
    }

    init {
        viewModelScope.launch {
            _plugin.state
                .map { it.isLoading }
                .distinctUntilChanged()
                .collect { isLoading ->
                    _state.update { it.copy(isLoading = isLoading) }
                }
        }
    }

    data class State(
        val content: String = "",
        val isLoading: Boolean = false,
    )
}