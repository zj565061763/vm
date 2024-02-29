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
    val userData by vm.data.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            modifier = Modifier.widthIn(100.dp),
            onClick = { vm.data.load() },
        ) {
            if (userData.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    color = LocalContentColor.current,
                    strokeWidth = 1.dp,
                )
            } else {
                Text(text = "load")
            }
        }

        AnimatedVisibility(visible = userData.isLoading) {
            Button(
                modifier = Modifier.widthIn(100.dp),
                onClick = { vm.data.cancelLoad() },
            ) {
                Text(text = "cancel")
            }
        }

        userData.data?.let {
            Text(text = it.name)
        }

        userData.result?.onSuccess {
            Text(text = "Success")
        }
        userData.result?.onFailure {
            Text(text = "Failure:$it")
        }
    }
}

class MyDataViewModel : PluginViewModel<Unit>() {

    /** 数据 */
    val data = DataPlugin { loadData() }.register()

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

        return Result.success(UserModel(name = uuid)).also {
            logMsg { "load data success  $uuid" }
        }
    }
}