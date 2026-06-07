package com.example.cpr_new

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cpr_new.core.di.AgentBackend
import com.example.cpr_new.core.di.ServiceLocator
import com.example.cpr_new.feature.session.CprSessionViewModel
import com.example.cpr_new.hardware.permission.CprPermissions
import com.example.cpr_new.hardware.permission.CprPermissions.RECORD_AUDIO
import com.example.cpr_new.hardware.permission.rememberPermissionState
import com.example.cpr_new.ui.screen.CprGuidanceScreen
import com.example.cpr_new.ui.theme.Cpr_newTheme

/**
 * 应用入口 —— 第 4 部分总装。
 *
 * 职责：搭好 Compose 宿主，向急救主屏注入会话 ViewModel，并在开始急救时
 * 顺带触发一次运行时权限请求。真正的业务编排都在 [CprSessionViewModel] 中。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Cpr_newTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CprApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/**
 * 顶层可组合：负责装配依赖、持有 ViewModel、桥接权限请求与屏幕回调。
 */
@Composable
private fun CprApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // 依赖在首次组合时装配一次（默认 Mock，可在 ServiceLocator 替换为真实实现）。
    val deps = remember { ServiceLocator.provideDependencies(context) }
    val viewModel: CprSessionViewModel = viewModel(factory = CprSessionViewModel.factory(deps))

    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissions = rememberPermissionState(CprPermissions.recommended)
    val micGranted = permissions.snapshot.isGranted(RECORD_AUDIO)

    LaunchedEffect(micGranted, state.isActive) {
        if (micGranted && state.isActive) viewModel.enableLiveCapture()
    }

    CprGuidanceScreen(
        state = state,
        onStart = {
            if (!permissions.snapshot.allGranted) permissions.request()
            val micGranted = permissions.snapshot.isGranted(RECORD_AUDIO)
            viewModel.startSession(micGranted = micGranted)
        },
        onStop = viewModel::stopSession,
        onDialEmergency = viewModel::dialEmergency,
        onDismissIncident = viewModel::dismissIncident,
        onDismissReport = viewModel::dismissReport,
        onPrimaryButton = viewModel::onPrimaryButtonClick,
        onQuickReply = viewModel::onQuickReply,
        showQuickReplies = ServiceLocator.agentBackend == AgentBackend.REMOTE_COPILOT,
        modifier = modifier,
        // 相机权限授予后翻转，CameraPreview 会自动绑定并显示预览。
        cameraGranted = permissions.snapshot.isGranted(CprPermissions.CAMERA),
        frameSink = viewModel.frameSink,
    )
}
