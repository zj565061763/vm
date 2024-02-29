package com.sd.demo.vm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sd.demo.vm.model.UserModel
import com.sd.lib.vm.PluginViewModel
import com.sd.lib.vm.plugin.DataPlugin
import com.sd.lib.vm.plugin.onData
import com.sd.lib.vm.plugin.onFailure
import com.sd.lib.vm.plugin.onSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.UUID

class SampleDataPlugin : ComponentActivity() {
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
    vm: MyDataViewModel = viewModel()
) {
    val state by vm.data.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            modifier = Modifier.widthIn(100.dp),
            onClick = { vm.data.load() },
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    color = LocalContentColor.current,
                    strokeWidth = 1.dp,
                )
            } else {
                Text(text = "load")
            }
        }

        AnimatedVisibility(visible = state.isLoading) {
            Button(
                modifier = Modifier.widthIn(100.dp),
                onClick = { vm.data.cancelLoad() },
            ) {
                Text(text = "cancel")
            }
        }

        state.onData {
            Text(text = it.name)
        }

        state.onSuccess {
            Text(text = "Success")
        }

        state.onFailure {
            Text(text = "Failure:$it")
        }
    }
}

class MyDataViewModel : PluginViewModel<Unit>() {

    /** 数据 */
    val data = DataPlugin { loadData() }.register()

    private var _count = 0

    override suspend fun handleIntent(intent: Unit) {}

    /**
     * 加载数据
     */
    private suspend fun loadData(): Result<UserModel> {
        val uuid = UUID.randomUUID().toString()
        logMsg { "load data start $uuid" }

        try {
            // 模拟加载数据
            delay(3_000)
        } catch (e: CancellationException) {
            logMsg { "load data cancel  $uuid" }
            throw e
        }

        val success = _count % 2 == 0
        _count++

        return if (success) {
            logMsg { "load data success  $uuid" }
            Result.success(UserModel(name = uuid))
        } else {
            logMsg { "load data failure  $uuid" }
            Result.failure(Exception("count $_count"))
        }
    }
}