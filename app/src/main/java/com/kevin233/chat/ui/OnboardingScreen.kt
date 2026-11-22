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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    Column(
        Modifier
            .fillMaxSize()
            .background(palette.bg)
            .statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 0) {
                IconButton(onClick = { step-- }) {
                    Icon(Icons.Rounded.ArrowBack, "返回", tint = palette.text, modifier = Modifier.size(21.dp))
                }
            } else {
                Spacer(Modifier.width(10.dp))
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                repeat(3) { i ->
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(if (i == step) 20.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (i <= step) palette.accent else palette.border)
                    )
                }
            }
            TextButton(onClick = { vm.skipOnboarding() }) {
                Text("先逛逛", color = palette.textSub, style = MaterialTheme.typography.labelMedium)
            }
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally(tween(260)) { it / 4 } + fadeIn(tween(220)))
                    .togetherWith(slideOutHorizontally(tween(200)) { -it / 6 } + fadeOut(tween(160)))
            },
            label = "onboarding",
            modifier = Modifier.weight(1f)
        ) { current ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
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
                                result = false to "先填上 Key 才能开始哦，或者点下面的「暂时跳过」。"
                            } else {
                                vm.finishOnboarding(preset, key.trim())
                                vm.screen = Screen.CHAT
                            }
                        },
                        onSkip = { vm.skipOnboarding() }
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    val palette = pal
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(AppGradient),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("AI Chat", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text("轻一点，省一点", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(26.dp))
        Text(
            "你的 AI 聊天工具\n终于不用折腾了",
            style = MaterialTheme.typography.headlineSmall,
            color = palette.text,
            lineHeight = 34.sp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "用自己的 API Key，直接连 DeepSeek 等几十家模型。全程只要 1 分钟，不写一行代码。",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSub
        )
        Spacer(Modifier.height(22.dp))
        FeatureRow(CacheStack, "自动省钱", "针对每家模型优化缓存，命中后输入价格最低 1 折")
        FeatureRow(Icons.Rounded.PhoneAndroid, "手机平板都顺手", "大屏两栏、小屏抽屉，单手也能用")
        FeatureRow(Icons.Rounded.Lock, "只存在本机", "聊天和 Key 都不上传，没有账号、没有广告")
        Spacer(Modifier.height(26.dp))
        PrimaryButton("开始，只要 1 分钟", onStart)
        Spacer(Modifier.height(12.dp))
        Text(
            "已经有 Key 了？点「先逛逛」，去设置里填也行",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSub,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    val palette = pal
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(palette.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = palette.accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = palette.text)
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
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(6.dp))
        Text("用哪家的模型？", style = MaterialTheme.typography.headlineSmall, color = palette.text)
        Spacer(Modifier.height(6.dp))
        Text(
            "不知道选哪个就选 DeepSeek：国内能直接连、便宜、还支持缓存打折。",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSub
        )
        Spacer(Modifier.height(18.dp))
        Surface(
            color = palette.accentSoft,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, palette.accent),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onSelect("deepseek")
                    onNext()
                }
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("DeepSeek", style = MaterialTheme.typography.titleMedium, color = palette.accentText)
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.clip(CircleShape).background(palette.accent).padding(horizontal = 7.dp, vertical = 2.dp)) {
                            Text("推荐", color = Color.White, fontSize = 9.sp)
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text("国内直连，便宜，缓存命中再打折", style = MaterialTheme.typography.labelSmall, color = palette.textSub)
                }
                Text("就用它", style = MaterialTheme.typography.labelLarge, color = palette.accent)
                Icon(Icons.Rounded.ChevronRight, null, tint = palette.accent, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("或者选别的平台", style = MaterialTheme.typography.labelMedium, color = palette.textSub)
        Spacer(Modifier.height(8.dp))
        val list = Presets.all.filter { it.id != "custom" && it.id != "deepseek" }
        list.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                row.forEach { p ->
                    val active = p.id == selected
                    Surface(
                        color = if (active) palette.accentSoft else palette.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            if (active) 1.5.dp else 1.dp,
                            if (active) palette.accent else palette.border
                        ),
                        modifier = Modifier.weight(1f).clickable { onSelect(p.id) }
                    ) {
                        Column(Modifier.padding(horizontal = 13.dp, vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    p.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (active) palette.accentText else palette.text,
                                    maxLines = 1
                                )
                                if (p.badge.isNotBlank()) {
                                    Spacer(Modifier.width(5.dp))
                                    Box(
                                        Modifier.clip(CircleShape).background(palette.accent).padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(p.badge, color = Color.White, fontSize = 9.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                p.desc,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSub,
                                maxLines = 2,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "没看到你用的平台？先跳过，去设置里加自定义接口就行。",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSub
        )
        Spacer(Modifier.height(18.dp))
        PrimaryButton("下一步", onNext)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onCustom, modifier = Modifier.fillMaxWidth()) {
            Text("全部平台 / 自定义接口", color = palette.accent, style = MaterialTheme.typography.labelMedium)
        }
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
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(6.dp))
        Text("拿到 ${preset.name} 的 Key", style = MaterialTheme.typography.headlineSmall, color = palette.text)
        Spacer(Modifier.height(6.dp))
        Text("跟着下面做，一步都不用想。", style = MaterialTheme.typography.bodyMedium, color = palette.textSub)
        Spacer(Modifier.height(16.dp))
        Surface(color = palette.surface, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)) {
            Column(Modifier.padding(16.dp)) {
                val guide = preset.guide.ifEmpty {
                    listOf(
                        "打开 ${preset.name} 的官网，注册并登录。",
                        "找到「API Keys」或「API 密钥」页面。",
                        "点「创建」，复制那串 Key（一般是 sk- 开头）。",
                        "回到这里粘贴，点下面的按钮测试。"
                    )
                }
                guide.forEachIndexed { i, line ->
                    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier.size(20.dp).clip(CircleShape).background(palette.accentSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${i + 1}", fontSize = 11.sp, color = palette.accent, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(line, style = MaterialTheme.typography.bodyMedium, color = palette.text)
                    }
                }
                if (preset.keyUrl.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.surfaceAlt)
                            .clickable { onOpenSite() }
                            .padding(horizontal = 12.dp, vertical = 11.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.OpenInNew, null, tint = palette.accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("打开 ${preset.name} 官网拿 Key", style = MaterialTheme.typography.labelLarge, color = palette.accent)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Surface(color = palette.surface, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (result?.first == false) palette.danger else palette.border)) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Key, null, tint = palette.textSub, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (key.isEmpty()) {
                            Text(
                                "粘贴到这里",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSub.copy(alpha = 0.7f)
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = key,
                            onValueChange = onKeyChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.text),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(palette.accent),
                            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    IconButton(onClick = onToggleShow, modifier = Modifier.size(34.dp)) {
                        Icon(
                            if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            "显示/隐藏",
                            tint = palette.textSub,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.accentSoft)
                            .clickable { onPaste() }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ContentPaste, null, tint = palette.accent, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("粘贴", style = MaterialTheme.typography.labelMedium, color = palette.accent)
                        }
                    }
                }
            }
        }
        if (result != null) {
            Spacer(Modifier.height(10.dp))
            Surface(
                color = if (result.first) palette.ok.copy(alpha = 0.09f) else palette.danger.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        if (result.first) Icons.Rounded.Check else Icons.Rounded.ErrorOutline,
                        null,
                        tint = if (result.first) palette.ok else palette.danger,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        result.second,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (result.first) palette.ok else palette.danger
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        PrimaryButton(
            text = if (testing) "正在测试…" else "测试并开始",
            onClick = onTest,
            enabled = !testing,
            loading = testing
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSkipSave, enabled = !testing, modifier = Modifier.fillMaxWidth()) {
            Text("先保存，不测试直接开始", color = palette.textSub, style = MaterialTheme.typography.labelMedium)
        }
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text("暂时跳过，我自己研究", color = palette.textSub, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, null, tint = palette.textSub, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                "Key 只保存在这台手机，不会经过任何第三方服务器。",
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
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) AppGradient else Brush.linearGradient(listOf(pal.textSub.copy(alpha = 0.3f), pal.textSub.copy(alpha = 0.3f))))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge, fontSize = 15.sp)
        }
    }
}
