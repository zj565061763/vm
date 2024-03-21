package com.sd.demo.vm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sd.demo.vm.model.UserModel
import com.sd.lib.compose.swiperefresh.FSwipeRefresh
import com.sd.lib.compose.swiperefresh.IndicatorMode
import com.sd.lib.compose.swiperefresh.rememberFSwipeRefreshState
import com.sd.lib.vm.PluginViewModel
import com.sd.lib.vm.plugin.PagePlugin
import com.sd.lib.vm.plugin.PageState
import com.sd.lib.vm.plugin.onViewFailureEmpty
import com.sd.lib.vm.plugin.onViewSuccessEmpty
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.UUID

class SamplePagePlugin : ComponentActivity() {
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
    vm: MyPageViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(vm) {
        vm.refreshData()
    }

    FSwipeRefresh(
        modifier = modifier,
        state = rememberFSwipeRefreshState {
            it.endIndicatorMode = IndicatorMode.Boundary
        },
        isRefreshingStart = state.isRefreshing,
        isRefreshingEnd = null,
        onRefreshStart = { vm.refreshData() },
        onRefreshEnd = { vm.loadMoreData() },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items = state.data, key = { it.name }) { user ->
                ItemView(user = user)
            }

            item(contentType = "footer") {
                FooterView(
                    modifier = Modifier.fillMaxWidth(),
                    pageState = state,
                )
            }
        }

        state.onViewSuccessEmpty {
            EmptyView()
        }

        state.onViewFailureEmpty {
            ErrorView(text = it.toString())
        }
    }
}

@Composable
private fun ItemView(
    modifier: Modifier = Modifier,
    user: UserModel,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primary)
            .fillMaxWidth()
            .height(50.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = user.name,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun EmptyView(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Empty View")
    }
}

@Composable
private fun ErrorView(
    modifier: Modifier = Modifier,
    text: String,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Error View $text")
    }
}

/**
 * 列表尾部
 */
@Composable
private fun FooterView(
    modifier: Modifier = Modifier,
    pageState: PageState<*>,
) {
    if (pageState.data.isEmpty()) return
    val hasMore = pageState.hasMore ?: return
    val isLoadingMore = pageState.isLoadingMore

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(50.dp)
            .padding(5.dp)
    ) {
        if (isLoadingMore) {
            // 加载中
            Text(
                text = "加载中...",
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        } else {
            if (hasMore) {
                // 还有数据
            } else {
                Text(
                    text = "没有更多数据了~",
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }
}

class MyPageViewModel : PluginViewModel<Unit>() {
    private val _listUser = mutableListOf<UserModel>()
    private val _plugin = plugin { PagePlugin<UserModel>() }

    val state = _plugin.state

    fun refreshData() {
        _plugin.refresh { loadUsers(it, true) }
    }

    fun loadMoreData() {
        _plugin.loadMore { loadUsers(it, false) }
    }

    /**
     * 加载用户分页数据
     */
    private suspend fun loadUsers(
        page: Int,
        isRefresh: Boolean,
    ): PagePlugin.LoadResult<UserModel> {

        val uuid = UUID.randomUUID().toString()
        logMsg { "load page:$page $uuid" }

        // 模拟加载数据
        try {
            delay(1_000)
        } catch (e: CancellationException) {
            logMsg { "load cancel $uuid" }
            throw e
        }

        if (isRefresh) {
            if (randomBoolean()) {
                _listUser.clear()
                return if (randomBoolean()) {
                    logMsg { "load empty success $uuid" }
                    PagePlugin.resultSuccess(
                        data = emptyList(),
                        pageSize = 0,
                        hasMore = false,
                    )
                } else {
                    logMsg { "load empty failure $uuid" }
                    PagePlugin.resultFailure(
                        exception = Exception("Connection timeout."),
                        data = emptyList(),
                    )
                }
            }
        }

        if (page > 3) {
            logMsg { "load no more data $uuid" }
            return PagePlugin.resultSuccess(
                data = null,
                pageSize = 0,
                hasMore = false,
            )
        }

        val list = Array(10) {
            UserModel(name = UUID.randomUUID().toString())
        }

        if (isRefresh) {
            // 刷新
            _listUser.clear()
            _listUser.addAll(list)
        } else {
            // 加载更多
            _listUser.addAll(list)
        }

        logMsg { "load success $uuid" }
        return PagePlugin.resultSuccess(
            data = _listUser.toList(),
            pageSize = list.size,
            hasMore = true,
        )
    }
}

private fun randomBoolean(): Boolean = listOf(true, false).random()