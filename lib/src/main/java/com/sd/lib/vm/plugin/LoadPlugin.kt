package com.sd.lib.vm.plugin

import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections

/**
 * 加载数据
 */
interface LoadPlugin : StateVMPlugin<StateFlow<LoadState>> {
    /**
     * 加载数据，如果上一次加载还未完成，再次调用此方法，会取消上一次加载
     *
     * @param notifyLoading 是否通知[LoadState.isLoading]
     * @param ignoreActive 是否忽略激活状态[VMPlugin.Support.isVMActive]
     * @param canLoad 是否可以加载
     * @param onLoad 加载回调
     */
    fun load(
        notifyLoading: Boolean = true,
        ignoreActive: Boolean = false,
        canLoad: suspend () -> Boolean = { true },
        onLoad: suspend () -> Unit,
    )

    /**
     * 取消加载
     */
    fun cancelLoad()
}

/**
 * [LoadPlugin]
 */
fun LoadPlugin(): LoadPlugin {
    return LoadPluginImpl()
}

//---------- state ----------

data class LoadState(
    /** 是否正在加载中 */
    val isLoading: Boolean = false,
)

//---------- impl ----------

private class LoadPluginImpl : RealVMPlugin(), LoadPlugin {
    /** 所有任务 */
    private val _jobs: MutableSet<Job> = Collections.synchronizedSet(hashSetOf())

    /** 加载中的任务 */
    @Volatile
    private var _loadingJob: Job? = null

    private val _state = MutableStateFlow(LoadState())
    override val state: StateFlow<LoadState> = _state.asStateFlow()

    override fun load(
        notifyLoading: Boolean,
        ignoreActive: Boolean,
        canLoad: suspend () -> Boolean,
        onLoad: suspend () -> Unit,
    ) {
        vmScope.launch {
            loadInternal(
                notifyLoading = notifyLoading,
                ignoreActive = ignoreActive,
                canLoad = canLoad,
                onLoad = onLoad,
            )
        }.let { job ->
            _jobs.add(job)
            job.invokeOnCompletion { _jobs.remove(job) }
        }
    }

    override fun cancelLoad() {
        _loadingJob?.cancel()
        while (_jobs.isNotEmpty()) {
            _jobs.toTypedArray().forEach { job ->
                _jobs.remove(job)
                job.cancel()
            }
        }
    }

    private suspend fun loadInternal(
        notifyLoading: Boolean,
        ignoreActive: Boolean,
        canLoad: suspend () -> Boolean,
        onLoad: suspend () -> Unit,
    ) {
        if (isVMDestroyed) return
        if (isVMActive || ignoreActive) {
            if (!canLoad()) return

            _loadingJob?.cancel()
            _loadingJob = currentCoroutineContext()[Job]

            try {
                if (notifyLoading) {
                    _state.update { it.copy(isLoading = true) }
                }
                onLoad()
            } finally {
                if (notifyLoading) {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}