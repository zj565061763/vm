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
import androidx.compose.material3.MaterialTheme
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
import com.sd.lib.vm.plugin.onFailure
import com.sd.lib.vm.plugin.onInitial
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
            onClick = {
                if (state.isLoading) {
                    // 取消加载
                    vm.data.cancelLoad()
                } else {
                    // 加载
                    vm.data.load()
                }
            },
        ) {
            if (state.isLoading) {
                Text(text = "cancel")
            } else {
                Text(text = "load")
            }
        }

        // 显示加载数据
        Text(text = state.data.name)

        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(10.dp),
                color = LocalContentColor.current,
                strokeWidth = 1.dp,
            )
        } else {
            state.onInitial {
                Text(text = "Initial")
            }
            state.onSuccess {
                Text(text = "Success")
            }
            state.onFailure {
                Text(
                    text = "Failure:$it",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

class MyDataViewModel : PluginViewModel<Unit>() {
    private var _count = 0

    /** 数据 */
    val data = DataPlugin(UserModel(name = "")) { loadData() }.register()

    /**
     * 加载数据
     */
    private suspend fun loadData(): Result<UserModel> {
        val uuid = UUID.randomUUID().toString()
        logMsg { "load start $uuid ($_count)" }

        try {
            // 模拟加载数据
            delay(2_000)
        } catch (e: CancellationException) {
            logMsg { "load cancel $uuid" }
            throw e
        }

        val count = _count
        val success = count % 2 == 0
        _count++

        return if (success) {
            logMsg { "load success $uuid" }
            Result.success(UserModel(name = uuid))
        } else {
            logMsg { "load failure $uuid" }
            Result.failure(Exception("count $count"))
        }
    }
}