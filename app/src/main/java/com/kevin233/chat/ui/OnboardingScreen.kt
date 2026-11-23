package com.kevin233.chat.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevin233.chat.ChatViewModel
import com.kevin233.chat.Screen
import com.kevin233.chat.data.ApiConfig
import com.kevin233.chat.data.Preset
import com.kevin233.chat.data.Presets

@Composable
fun OnboardingScreen(vm: ChatViewModel) {
    val palette = pal
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var presetId by rememberSaveable { mutableStateOf("deepseek") }
    var key by rememberSaveable { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val preset = Presets.byId(presetId) ?: Presets.all.first()
    val clipboard = LocalClipboardManager.current

    Column(Modifier.fillMaxSize().background(palette.bg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 0) {
                IconButton(
                    onClick = { step-- },
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(palette.surfaceAlt)
                ) {
                    Icon(Icons.Rounded.ArrowBack, "返回", tint = palette.text, modifier = Modifier.size(20.dp))
                }
            } else {
                AppLogo(28.dp, 9.dp)
            }
            Spacer(Modifier.width(12.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(3) { i ->
                    Box(
                        Modifier
                            .width(if (i == step) 28.dp else 10.dp)
                            .height(5.dp)
                            .background(if (i <= step) palette.accent else palette.border, RoundedCornerShape(50))
                    )
                }
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(palette.surfaceAlt)
                    .clickable { vm.skipOnboarding() }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text("跳过", style = MaterialTheme.typography.labelMedium, color = palette.textSub)
            }
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally(tween(260)) { it / 5 } + fadeIn(tween(220)))
                    .togetherWith(slideOutHorizontally(tween(200)) { -it / 8 } + fadeOut(tween(160)))
            },
            label = "onboarding",
            modifier = Modifier.weight(1f)
        ) { current ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                when (current) {
                    0 -> WelcomeStep(onStart = { step = 1 })
                    1 -> ChooseStep(
                        selected = presetId,
                        onSelect = {
                            presetId = it
                            result = null
                        },
                        onNext = { step = 2 },
                        onCustom = {
                            vm.skipOnboarding()
                            vm.screen = Screen.SETTINGS
                            vm.editingConfig = ApiConfig(name = "", type = "openai")
                        }
                    )
                    else -> KeyStep(
                        preset = preset,
                        key = key,
                        onKeyChange = {
                            key = it
                            result = null
                        },
                        showKey = showKey,
                        onToggleShow = { showKey = !showKey },
                        onPaste = {
                            clipboard.getText()?.text?.trim()?.let {
                                key = it
                                result = null
                            }
                        },
                        testing = testing,
                        result = result,
                        onOpenSite = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(preset.keyUrl)))
                            }
                        },
                        onTest = {
                            if (key.isBlank()) {
                                result = false to "还没粘贴 Key。先点上面的按钮去拿一个。"
                            } else {
                                testing = true
                                result = null
                                vm.testAndStart(preset, key.trim()) { ok, msg ->
                                    testing = false
                                    result = ok to msg
                                    if (ok) vm.screen = Screen.CHAT
                                }
                            }
                        },
                        onSkipSave = {
                            if (key.isBlank()) {
                                result = false to "先填上 Key 才能开始，或者点下面的跳过。"
                            } else {
                                vm.finishOnboarding(preset, key.trim())
                                vm.screen = Screen.CHAT
                            }
                        },
                        onSkip = { vm.skipOnboarding() }
                    )
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    val palette = pal
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        Row(
            Modifier.background(palette.accentSoft, RoundedCornerShape(50))
                .padding(horizontal = 11.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(6.dp).background(palette.accent, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text("1 分钟完成配置", style = MaterialTheme.typography.labelMedium, color = palette.accentText)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "把 AI 装进手机",
            style = MaterialTheme.typography.displaySmall,
            fontSize = 38.sp,
            lineHeight = 46.sp,
            color = palette.text
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "用自己的 API Key，直接连 DeepSeek 等几十家模型。不写代码，不注册账号，全程只要一分钟。",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSub
        )
        Spacer(Modifier.height(24.dp))
        FeatureRow(1, "自己的 Key", "直接连模型，没有中间商，也没有订阅费")
        Spacer(Modifier.height(8.dp))
        FeatureRow(2, "缓存省钱", "按厂商优化前缀缓存，命中后输入价格更低")
        Spacer(Modifier.height(8.dp))
        FeatureRow(3, "只存本机", "聊天和 Key 都不会上传，没有账号和广告")
        Spacer(Modifier.height(24.dp))
        PrimaryButton("开始配置", onStart)
        Spacer(Modifier.height(10.dp))
        Text(
            "已经有 Key？点右上角跳过，设置里也能填。",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSub,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FeatureRow(number: Int, title: String, desc: String) {
    val palette = pal
    Row(
        Modifier.fillMaxWidth()
            .background(palette.surface, RoundedCornerShape(16.dp))
            .border(1.dp, palette.border, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier.size(34.dp).background(palette.accentSoft, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = palette.accent
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = palette.text)
            Spacer(Modifier.height(2.dp))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = palette.textSub)
        }
    }
}

@Composable
private fun ChooseStep(
    selected: String,
    onSelect: (String) -> Unit,
    onNext: () -> Unit,
    onCustom: () -> Unit
) {
    val palette = pal
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        Text("选择模型入口", style = MaterialTheme.typography.headlineMedium, color = palette.text)
        Spacer(Modifier.height(8.dp))
        Text(
            "不知道选哪个就选 DeepSeek：国内能直接连，便宜，缓存命中再打折。",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSub
        )
        Spacer(Modifier.height(20.dp))
        val deepSeek = Presets.byId("deepseek")
        if (deepSeek != null) {
            Row(
                Modifier.fillMaxWidth()
                    .background(palette.accentSoft, RoundedCornerShape(16.dp))
                    .border(1.dp, palette.accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .clickable {
                        onSelect(deepSeek.id)
                        onNext()
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(deepSeek.name, style = MaterialTheme.typography.titleMedium, color = palette.accentText)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "推荐",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accent,
                            modifier = Modifier
                                .background(palette.surface, RoundedCornerShape(50))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(deepSeek.desc, style = MaterialTheme.typography.bodySmall, color = palette.textSub)
                }
                Text("就用它 →", style = MaterialTheme.typography.labelMedium, color = palette.accent)
            }
        }
        Spacer(Modifier.height(18.dp))
        SectionLabel("全部平台")
        Spacer(Modifier.height(6.dp))
        val list = Presets.all.filter { it.id != "custom" && it.id != "deepseek" }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            list.forEach { p ->
                Row(
                    Modifier.fillMaxWidth()
                        .background(palette.surface, RoundedCornerShape(16.dp))
                        .border(
                            if (p.id == selected) 2.dp else 1.dp,
                            if (p.id == selected) palette.accent else palette.border,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelect(p.id) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p.name, style = MaterialTheme.typography.titleSmall, color = palette.text)
                            if (p.id == selected) {
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    "已选",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.accent,
                                    modifier = Modifier
                                        .background(palette.accentSoft, RoundedCornerShape(50))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(p.desc, style = MaterialTheme.typography.bodySmall, color = palette.textSub, maxLines = 2)
                    }
                    Text("→", style = MaterialTheme.typography.labelMedium, color = palette.textSub, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onCustom) {
            Text("自定义接口 / 中转站 →", style = MaterialTheme.typography.labelMedium, color = palette.accent)
        }
        Spacer(Modifier.height(8.dp))
        PrimaryButton("下一步", onNext)
    }
}

@Composable
private fun KeyStep(
    preset: Preset,
    key: String,
    onKeyChange: (String) -> Unit,
    showKey: Boolean,
    onToggleShow: () -> Unit,
    onPaste: () -> Unit,
    testing: Boolean,
    result: Pair<Boolean, String>?,
    onOpenSite: () -> Unit,
    onTest: () -> Unit,
    onSkipSave: () -> Unit,
    onSkip: () -> Unit
) {
    val palette = pal
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        Text("拿到 ${preset.name} 的 Key", style = MaterialTheme.typography.headlineMedium, color = palette.text)
        Spacer(Modifier.height(7.dp))
        Text("跟着下面做，一步都不用想。", style = MaterialTheme.typography.bodyMedium, color = palette.textSub)
        Spacer(Modifier.height(16.dp))
        val guide = preset.guide.ifEmpty {
            listOf(
                "打开 ${preset.name} 的官网，注册并登录。",
                "找到 API Keys 或 API 密钥页面。",
                "点创建，复制那串 Key。",
                "回到这里粘贴，点下面的按钮测试。"
            )
        }
        Column(
            Modifier.fillMaxWidth()
                .background(palette.surface, RoundedCornerShape(18.dp))
                .border(1.dp, palette.border, RoundedCornerShape(18.dp))
                .padding(4.dp)
        ) {
            guide.forEachIndexed { index, line ->
                Row(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.size(24.dp).background(palette.accentSoft, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (index + 1).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accent
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(line, style = MaterialTheme.typography.bodySmall, color = palette.text)
                }
                if (index < guide.lastIndex) {
                    Box(Modifier.fillMaxWidth().padding(start = 44.dp).height(1.dp).background(palette.border))
                }
            }
        }
        if (preset.keyUrl.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().clickable { onOpenSite() }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.OpenInNew, null, tint = palette.accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "打开 ${preset.name} 官网拿 Key",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accent,
                    modifier = Modifier.weight(1f)
                )
                Text("↗", style = MaterialTheme.typography.labelMedium, color = palette.accent)
            }
        }
        Spacer(Modifier.height(20.dp))
        SectionLabel("粘贴 Key")
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth()
                .background(palette.surfaceAlt, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f).padding(vertical = 7.dp)) {
                if (key.isEmpty()) {
                    Text(
                        "粘贴到这里",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSub.copy(alpha = 0.65f)
                    )
                }
                BasicTextField(
                    value = key,
                    onValueChange = onKeyChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.text),
                    cursorBrush = SolidColor(palette.accent),
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            IconButton(onClick = onToggleShow, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    "显示/隐藏",
                    tint = palette.textSub,
                    modifier = Modifier.size(17.dp)
                )
            }
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).clickable { onPaste() }.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.ContentPaste, null, tint = palette.accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text("粘贴", style = MaterialTheme.typography.labelSmall, color = palette.accent)
            }
        }
        if (result != null) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth()
                    .background(
                        if (result.first) palette.ok.copy(alpha = 0.07f) else palette.danger.copy(alpha = 0.06f),
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        1.dp,
                        if (result.first) palette.ok.copy(alpha = 0.3f) else palette.danger.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    if (result.first) Icons.Rounded.Check else Icons.Rounded.ErrorOutline,
                    null,
                    tint = if (result.first) palette.ok else palette.danger,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    result.second,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (result.first) palette.ok else palette.danger
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(if (testing) "正在测试…" else "测试并开始", onTest, enabled = !testing, loading = testing)
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onSkipSave, enabled = !testing, modifier = Modifier.fillMaxWidth()) {
            Text("先保存，不测试直接开始", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
        }
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text("暂时跳过，我自己研究", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, null, tint = palette.textSub, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Key 只保存在这台手机，不经过任何第三方服务器。",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSub
            )
        }
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val palette = pal
    Row(
        Modifier.fillMaxWidth().height(52.dp)
            .background(if (enabled) palette.accent else palette.surfaceAlt, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp, color = palette.onAccent)
            Spacer(Modifier.width(9.dp))
        }
        Text(
            text,
            color = if (enabled) palette.onAccent else palette.textSub,
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(Modifier.weight(1f))
        Text(
            "→",
            color = if (enabled) palette.onAccent else palette.textSub,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
