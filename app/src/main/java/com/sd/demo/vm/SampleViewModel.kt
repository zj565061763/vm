package com.sd.demo.vm

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sd.demo.vm.plugin.ToastPlugin
import com.sd.lib.vm.PluginViewModel
import kotlinx.coroutines.flow.Flow
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
    val context = LocalContext.current

    LifecycleStartEffect(vm) {
        // 设置ViewModel的激活状态
        vm.setActive(true)
        onStopOrDispose { vm.setActive(false) }
    }

    LaunchedEffect(Unit) {
        vm.toast.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(onClick = {
            vm.dispatch(MyViewModel.Intent.Login)
        }) {
            Text(text = "login")
        }
    }
}

internal class MyViewModel : PluginViewModel<MyViewModel.Intent>() {

    private val _toast = ToastPlugin().register()
    val toast: Flow<String> = _toast.state

    override suspend fun handleIntent(intent: Intent) {
        super.handleIntent(intent)
        logMsg { "handleIntent:$intent ${Thread.currentThread().name}" }
        when (intent) {
            Intent.Login -> _toast.showToast("login")
        }
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