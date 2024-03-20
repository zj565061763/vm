package com.sd.lib.vm.plugin

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Collections

/**
 * 加载数据
 */
interface LoadPlugin : VMPlugin {

    /** 是否正在加载中 */
    val isLoading: StateFlow<Boolean>

    /**
     * 加载数据，如果上一次加载还未完成，再次调用此方法，会取消上一次加载
     *
     * @param notifyLoading 是否通知[isLoading]
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
fun LoadPlugin(): LoadPlugin = LoadPluginImpl()

private class LoadPluginImpl : RealVMPlugin(), LoadPlugin {
    /** 所有任务 */
    private val _jobs: MutableSet<Job> = Collections.synchronizedSet(hashSetOf())

    /** 加载中的任务 */
    @Volatile
    private var _loadingJob: Job? = null

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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

            _loadingJob?.cancelAndJoin()
            _loadingJob = currentCoroutineContext()[Job]

            try {
                if (notifyLoading) {
                    _isLoading.value = true
                }
                onLoad()
            } finally {
                if (notifyLoading) {
                    _isLoading.value = false
                }
            }
        }
    }
}