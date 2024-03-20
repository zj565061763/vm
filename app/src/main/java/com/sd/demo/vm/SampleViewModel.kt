package com.sd.demo.vm

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sd.demo.vm.plugin.LoadingDialogPlugin
import com.sd.demo.vm.plugin.ToastPlugin
import com.sd.lib.vm.PluginViewModel
import com.sd.lib.vm.onActive
import com.sd.lib.vm.onInActive
import kotlinx.coroutines.delay

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
    val stateLoadingDialog by vm.stateLoadingDialog.collectAsStateWithLifecycle()

    // 设置ViewModel的激活状态
    LifecycleStartEffect(vm) {
        vm.setActive(true)
        onStopOrDispose { vm.setActive(false) }
    }

    // toast
    LaunchedEffect(vm) {
        vm.stateToast.collect { state ->
            Toast.makeText(context, state.msg, Toast.LENGTH_SHORT).show()
        }
    }

    // loading dialog
    stateLoadingDialog?.let { LoadingDialog(msg = it.msg) }

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

/**
 * 加载框
 */
@Composable
private fun LoadingDialog(
    modifier: Modifier = Modifier,
    msg: String,
) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        )
    ) {
        Column(modifier = modifier) {
            CircularProgressIndicator()
            Text(text = msg)
        }
    }
}

class MyViewModel : PluginViewModel<MyViewModel.Intent>() {

    private val _toast = ToastPlugin().register()
    private val _loadingDialog = LoadingDialogPlugin().register()

    val stateToast = _toast.state
    val stateLoadingDialog = _loadingDialog.state

    override suspend fun handleIntent(intent: Intent) {
        super.handleIntent(intent)
        when (intent) {
            Intent.Login -> handleLogin()
        }
    }

    private suspend fun handleLogin() {
        try {
            _toast.showToast("login")
            _loadingDialog.showLoading("login...")
            delay(5_000)
        } finally {
            _loadingDialog.hideLoading()
            _toast.showToast("login success")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        logMsg { "onDestroy ${Thread.currentThread().name}" }
    }

    init {
        onActive {
            logMsg { "onActive ${Thread.currentThread().name}" }
        }
        onInActive {
            logMsg { "onInActive ${Thread.currentThread().name}" }
        }
    }

    sealed interface Intent {
        data object Login : Intent
    }
}