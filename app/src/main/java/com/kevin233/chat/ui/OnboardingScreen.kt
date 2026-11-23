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
                IconButton(onClick = { step-- }) {
                    Icon(Icons.Rounded.ArrowBack, "返回", tint = palette.text, modifier = Modifier.size(21.dp))
                }
            } else {
                AppLogo(26.dp, 8.dp)
            }
            Spacer(Modifier.width(12.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i ->
                    Box(
                        Modifier
                            .width(if (i == step) 26.dp else 10.dp)
                            .height(2.dp)
                            .background(if (i <= step) palette.accent else palette.border)
                    )
                }
            }
            TextButton(onClick = { vm.skipOnboarding() }) {
                Text("SKIP", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
            }
        }
        Hairline()
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
        Kicker("AI CHAT / SETUP")
        Spacer(Modifier.height(12.dp))
        Text(
            "把 AI\n装进手机",
            style = MaterialTheme.typography.displaySmall,
            fontSize = 40.sp,
            lineHeight = 48.sp,
            color = palette.text
        )
        Spacer(Modifier.height(16.dp))
        Box(Modifier.width(52.dp).height(2.dp).background(palette.accent))
        Spacer(Modifier.height(18.dp))
        Text(
            "用自己的 API Key，直接连 DeepSeek 等几十家模型。不写代码，不注册账号，全程只要一分钟。",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSub
        )
        Spacer(Modifier.height(28.dp))
        FeatureRow(1, "自己的 Key", "直接连模型，没有中间商，也没有订阅费")
        FeatureRow(2, "缓存省钱", "按厂商优化前缀缓存，命中后输入价格更低")
        FeatureRow(3, "只存本机", "聊天和 Key 都不上传，没有账号和广告")
        Spacer(Modifier.height(26.dp))
        PrimaryButton("开始配置 →", onStart)
        Spacer(Modifier.height(10.dp))
        Text(
            "已经有 Key？点右上角 SKIP，设置里也能填。",
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
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
            Text(
                number.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelMedium,
                color = palette.accent,
                modifier = Modifier.width(30.dp)
            )
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = palette.text, fontWeight = FontWeight.Medium)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = palette.textSub)
            }
        }
        Hairline()
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
        Kicker("STEP 01 / MODEL")
        Spacer(Modifier.height(10.dp))
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
                    .background(palette.accentSoft, RoundedCornerShape(3.dp))
                    .border(1.dp, palette.accent.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
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
                        Text("推荐", style = MaterialTheme.typography.labelSmall, color = palette.accent)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(deepSeek.desc, style = MaterialTheme.typography.bodySmall, color = palette.textSub)
                }
                Text("就用它 →", style = MaterialTheme.typography.labelMedium, color = palette.accent)
            }
        }
        Spacer(Modifier.height(20.dp))
        Kicker("ALL PLATFORMS / 全部平台")
        Spacer(Modifier.height(4.dp))
        val list = Presets.all.filter { it.id != "custom" && it.id != "deepseek" }
        list.forEachIndexed { index, p ->
            Row(
                Modifier.fillMaxWidth().clickable {
                    onSelect(p.id)
                }.padding(vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    (index + 2).toString().padStart(2, '0'),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSub,
                    modifier = Modifier.width(28.dp)
                )
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.name, style = MaterialTheme.typography.titleSmall, color = palette.text)
                        if (p.id == selected) {
                            Spacer(Modifier.width(7.dp))
                            Text("已选", style = MaterialTheme.typography.labelSmall, color = palette.accent)
                        }
                    }
                    Text(p.desc, style = MaterialTheme.typography.bodySmall, color = palette.textSub, maxLines = 2)
                }
                Text("↗", style = MaterialTheme.typography.labelMedium, color = palette.textSub)
            }
            Hairline()
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onCustom) {
            Text("自定义接口 / 中转站 →", style = MaterialTheme.typography.labelMedium, color = palette.accent)
        }
        Spacer(Modifier.height(8.dp))
        PrimaryButton("下一步 →", onNext)
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
        Kicker("STEP 03 / API KEY")
        Spacer(Modifier.height(10.dp))
        Text("拿到 ${preset.name} 的 Key", style = MaterialTheme.typography.headlineMedium, color = palette.text)
        Spacer(Modifier.height(8.dp))
        Text("跟着下面做，一步都不用想。", style = MaterialTheme.typography.bodyMedium, color = palette.textSub)
        Spacer(Modifier.height(18.dp))
        val guide = preset.guide.ifEmpty {
            listOf(
                "打开 ${preset.name} 的官网，注册并登录。",
                "找到 API Keys 或 API 密钥页面。",
                "点创建，复制那串 Key。",
                "回到这里粘贴，点下面的按钮测试。"
            )
        }
        guide.forEachIndexed { index, line ->
            Row(Modifier.padding(vertical = 9.dp), verticalAlignment = Alignment.Top) {
                Text(
                    (index + 1).toString().padStart(2, '0'),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accent,
                    modifier = Modifier.width(30.dp)
                )
                Text(line, style = MaterialTheme.typography.bodyMedium, color = palette.text)
            }
            Hairline()
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
            Hairline()
        }
        Spacer(Modifier.height(20.dp))
        Kicker("PASTE / 粘贴 Key")
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
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
                Modifier.clip(RoundedCornerShape(2.dp)).clickable { onPaste() }.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.ContentPaste, null, tint = palette.accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text("粘贴", style = MaterialTheme.typography.labelSmall, color = palette.accent)
            }
        }
        Hairline()
        if (result != null) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth()
                    .background(
                        if (result.first) palette.ok.copy(alpha = 0.07f) else palette.danger.copy(alpha = 0.06f),
                        RoundedCornerShape(3.dp)
                    )
                    .border(
                        1.dp,
                        if (result.first) palette.ok.copy(alpha = 0.3f) else palette.danger.copy(alpha = 0.3f),
                        RoundedCornerShape(3.dp)
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
        PrimaryButton(if (testing) "正在测试…" else "测试并开始 →", onTest, enabled = !testing, loading = testing)
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
            .background(if (enabled) palette.accent else palette.surfaceAlt, RoundedCornerShape(3.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp, color = Color.White)
            Spacer(Modifier.width(9.dp))
        }
        Text(
            text,
            color = if (enabled) Color.White else palette.textSub,
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(Modifier.weight(1f))
        Text(
            "→",
            color = if (enabled) Color.White else palette.textSub,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
