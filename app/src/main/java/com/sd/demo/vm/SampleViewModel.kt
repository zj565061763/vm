package com.sd.demo.vm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sd.lib.vm.FViewModel
import kotlinx.coroutines.launch

class SampleViewModel : ComponentActivity() {
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
    vm: MyViewModel = viewModel()
) {
    LifecycleStartEffect(vm) {
        // 设置ViewModel的激活状态
        vm.setActive(true)
        onStopOrDispose { vm.setActive(false) }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(onClick = {
            vm.dispatch(MyViewModel.Intent.Login)
        }) {
            Text(text = "login")
        }
    }
}

internal class MyViewModel : FViewModel<MyViewModel.Intent>() {
    override suspend fun handleIntent(intent: Intent) {
        super.handleIntent(intent)
        logMsg { "handleIntent:$intent ${Thread.currentThread().name}" }
    }

    override fun onDestroy() {
        super.onDestroy()
        logMsg { "onDestroy ${Thread.currentThread().name}" }
    }

    init {
        viewModelScope.launch {
            isActiveFlow.collect {
                logMsg { "active:$it ${Thread.currentThread().name}" }
            }
        }
    }

    sealed interface Intent {
        data object Login : Intent
    }
}