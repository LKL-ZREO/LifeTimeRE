package com.lifetimere.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.lifetimere.app.domain.AiReviewSettings
import com.lifetimere.app.domain.AiConnectionMode
import com.lifetimere.app.ui.AiConnectionUiState
import com.lifetimere.app.ui.components.Page
import com.lifetimere.app.ui.components.PixelPanel
import com.lifetimere.app.ui.components.PixelTag
import com.lifetimere.app.ui.theme.PixelCoral
import com.lifetimere.app.ui.theme.PixelForest
import com.lifetimere.app.ui.theme.PixelGold
import com.lifetimere.app.ui.theme.PixelMuted
import com.lifetimere.app.ui.theme.PixelOutline
import com.lifetimere.app.ui.theme.PixelParchment
import com.lifetimere.app.ui.theme.PixelTeal

@Composable
fun SettingsScreen(
    settings: AiReviewSettings,
    connectionState: AiConnectionUiState,
    onBack: () -> Unit,
    onSave: (AiReviewSettings) -> Unit,
    onTestConnection: (AiReviewSettings) -> Unit,
) {
    var enabled by remember(settings.enabled) { mutableStateOf(settings.enabled) }
    var serviceUrl by remember(settings.serviceUrl) { mutableStateOf(settings.serviceUrl) }
    var accessToken by remember(settings.accessToken) { mutableStateOf(settings.accessToken) }
    var mode by remember(settings.mode) { mutableStateOf(settings.mode) }
    var settlementMinuteOfDay by remember(settings.settlementMinuteOfDay) {
        mutableStateOf(settings.settlementMinuteOfDay)
    }
    var showToken by remember { mutableStateOf(false) }
    val draft = AiReviewSettings(enabled, serviceUrl, accessToken, settlementMinuteOfDay, mode)
    val scheduleOptions = AiReviewSettings.SUPPORTED_SETTLEMENT_MINUTES
    val scheduleIndex = scheduleOptions.indexOf(settlementMinuteOfDay).coerceAtLeast(0)

    Page(
        title = "设置",
        subtitle = "管理 AI 回响服务和旅志传输。",
        action = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回今日旅志", tint = PixelGold)
            }
        },
    ) {
            PixelPanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("AI 回响", style = MaterialTheme.typography.titleLarge, color = PixelParchment)
                        Text("启用后，每天 ${draft.settlementTimeLabel} 后自动汇总已保存的记录，生成回顾和行动依据。", color = PixelMuted)
                    }
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
            }

            PixelPanel {
                Text("服务连接", style = MaterialTheme.typography.titleMedium, color = PixelTeal)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { mode = AiConnectionMode.DEEPSEEK }, modifier = Modifier.weight(1f)) {
                        Text(if (mode == AiConnectionMode.DEEPSEEK) "✓ DeepSeek 直连" else "DeepSeek 直连")
                    }
                    OutlinedButton(onClick = { mode = AiConnectionMode.GATEWAY }, modifier = Modifier.weight(1f)) {
                        Text(if (mode == AiConnectionMode.GATEWAY) "✓ 自建网关" else "自建网关")
                    }
                }
                if (mode == AiConnectionMode.GATEWAY) {
                OutlinedTextField(
                    value = serviceUrl,
                    onValueChange = { serviceUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Uri,
                    ),
                    label = { Text("AI 服务地址") },
                    placeholder = { Text("https://ai.example.com") },
                    supportingText = { Text("模拟器连接电脑本机可填写 http://10.0.2.2:8787") },
                )
                }
                OutlinedTextField(
                    value = accessToken,
                    onValueChange = { accessToken = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Password,
                    ),
                    label = { Text(if (mode == AiConnectionMode.DEEPSEEK) "DeepSeek API Key" else "网关访问令牌") },
                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showToken = !showToken }, enabled = enabled) {
                            Icon(
                                if (showToken) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (showToken) "隐藏令牌" else "显示令牌",
                            )
                        }
                    },
                    supportingText = { Text(if (mode == AiConnectionMode.DEEPSEEK) "Key 由你在此输入，使用 Android Keystore 加密保存在本机，不会写入 APK。" else "模型供应商密钥只保存在网关服务端。") },
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { onSave(draft) },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, PixelOutline),
                    ) { Text("保存") }
                    Button(
                        onClick = { onTestConnection(draft) },
                        enabled = enabled && !connectionState.isTesting,
                        modifier = Modifier.weight(1f),
                    ) { Text(if (connectionState.isTesting) "连接中" else "测试连接") }
                }
                ConnectionMessage(connectionState)
            }
        PixelPanel {
            Text("每天最晚汇总时间", style = MaterialTheme.typography.titleMedium, color = PixelTeal)
            Text("到达这个时间后，营火会自动汇总当天已保存的足迹；手动提前汇总不受影响。", color = PixelParchment)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { settlementMinuteOfDay = scheduleOptions[scheduleIndex - 1] },
                    enabled = scheduleIndex > 0,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, PixelOutline),
                ) { Text("提前 30 分") }
                PixelTag(draft.settlementTimeLabel, PixelGold)
                OutlinedButton(
                    onClick = { settlementMinuteOfDay = scheduleOptions[scheduleIndex + 1] },
                    enabled = scheduleIndex < scheduleOptions.lastIndex,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, PixelOutline),
                ) { Text("延后 30 分") }
            }
            Text("可选范围 18:00–23:30。修改后，尚未执行的汇总会按新时间重新安排。", color = PixelMuted)
            Button(onClick = { onSave(draft) }, modifier = Modifier.fillMaxWidth()) {
                Text("保存汇总时间")
            }
        }

        PixelPanel {
            Text("旅志如何处理", style = MaterialTheme.typography.titleMedium, color = PixelTeal)
            Text(
                if (mode == AiConnectionMode.DEEPSEEK) "自动或手动汇总时，当天已保存的全部记录、日期和修订号会直接发送给 DeepSeek。白天记下一笔只保存到本机；${draft.settlementTimeLabel} 后补记会触发当天重新汇总。"
                else "自动或手动汇总时，当天已保存的全部记录、日期和修订号会发送到你配置的服务。白天记下一笔只保存到本机；${draft.settlementTimeLabel} 后补记会触发当天重新汇总。",
                color = PixelParchment,
            )
            Text("服务返回摘要、行动属性、强度和原文引用；应用会再次校验并按固定规则结算点数。", color = PixelMuted)
            Text("关闭 AI 后仍可在本机保存、编辑和删除旅志。", color = PixelMuted)
        }
    }
}

@Composable
private fun ConnectionMessage(connectionState: AiConnectionUiState) {
    connectionState.message?.let { message ->
        PixelTag(
            text = message,
            color = when (connectionState.successful) {
                true -> PixelForest
                false -> PixelCoral
                null -> PixelGold
            },
        )
    }
}
