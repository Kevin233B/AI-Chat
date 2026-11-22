# AI Chat

一个约 1.4MB 的安卓 AI 聊天应用。自己的 API Key，直接连 DeepSeek、Claude、Gemini 等几十家模型。没有账号，没有广告，聊天记录只留在本机。

[![Android CI](https://github.com/Kevin233B/AI-Chat/actions/workflows/android.yml/badge.svg)](https://github.com/Kevin233B/AI-Chat/actions/workflows/android.yml)

- 包名：`com.kevin233.chat`
- 应用名：AI Chat
- 系统要求：Android 7.0 及以上
- 技术栈：Kotlin，Jetpack Compose，单 Activity
- 开源协议：MIT

## 功能

- 手把手引导配置 DeepSeek API，复制粘贴就能用
- 内置 DeepSeek、硅基流动、Kimi、智谱、通义、火山方舟、OpenAI、OpenRouter、Groq、Claude、Gemini、Ollama、LM Studio 等预设
- 支持任何 OpenAI 兼容接口，也支持 Claude 和 Gemini 原生接口
- 支持自定义系统提示词和固定资料
- 内置 9 个智能体，也可以自己创建人设
- 支持图片输入、图片显示和保存
- 支持思考强度调节，包含 low、medium、high、xhigh、max
- 支持联网搜索，按对话开关
- 针对不同厂商优化提示词缓存，显示每次命中和节省的 token
- 对话历史浏览、全文搜索、消息定位
- 对话导出与导入，JSON 格式，不包含 API Key
- 壁纸、玻璃透明度、深浅色主题
- 手机抽屉布局，平板自动双栏

## 安装

仓库不提供已签名的安装包。签名密钥只在开发者本机保存，不会上传到 GitHub。

### 方式一：自己构建 debug 包

debug 包使用系统调试签名，可以直接安装，适合自己用。

```bash
./gradlew :app:assembleDebug
```

输出在 `app/build/outputs/apk/debug/app-debug.apk`。

### 方式二：用 CI 的未签名 release 包，然后本地签名

1. 打开 GitHub Actions 的 `Android CI` 工作流
2. 下载 `AIChat-unsigned` 工件
3. 在本机准备一个签名密钥，并创建 `signing.properties`
4. 运行 `scripts/sign-local.sh` 完成签名

签名步骤：

```bash
cp signing.properties.example signing.properties
```

把 `signing.properties` 里的路径和密码改成你自己的，密钥文件不要放进仓库。

```bash
./scripts/sign-local.sh path/to/app-release-unsigned.apk
```

签名后的 APK 默认输出到 `dist/AIChat-signed.apk`。

`signing.properties`、`*.jks`、`*.keystore`、`local.properties`、`dist/` 都已经写进 `.gitignore`。请不要把签名材料、API Key 或聊天记录提交到仓库。

## 第一次配置 DeepSeek

1. 打开应用，点“开始，只要 1 分钟”
2. 点 DeepSeek 推荐卡片
3. 点按钮打开 DeepSeek 开放平台
4. 注册登录后点左侧“API keys”，创建并复制 `sk-` 开头的 Key
5. 回到应用点“粘贴”，再点“测试并开始”
6. 看到连接成功就能聊天

新手教程可以随时在“设置”里的“帮助与关于”重新打开。

## 智能体与人设

左侧抽屉里点“智能体与人设”，或者从设置页进入。

内置 9 个角色：

- 通用助手
- 翻译官
- 写作教练
- 编程搭档
- 英语陪练
- 面试官
- 学习导师
- 旅行规划师
- 产品经理

点一下就会新建一个带人设的对话。人设会固定成这个对话的系统提示词，之后不会因为改全局设置而变。

也可以点“创建”自定义智能体：头像 emoji、名字、一句话介绍、人设提示词、默认联网和默认思考强度。自定义智能体支持编辑和删除，内置的不能改。

## 图片

在模型设置里打开“这个模型支持图片”，聊天输入框才会出现图片按钮。

- 一次最多 4 张，发送前自动压缩
- OpenAI、Claude、Gemini 分别按官方图片格式发送
- 用户图片会显示在气泡里，点开可以全屏查看和保存
- 模型回复里的 Markdown 图片会直接显示
- Gemini 图片生成模型返回的图片会自动转成可显示的图片

图片是否可用取决于模型本身。不支持时，接口会返回错误，应用会显示原始提示。

## 思考强度

输入框上方有“思考”按钮，设置里的“生成设置”可以修改新对话的默认值。

档位顺序：

`low < medium < high < xhigh < max`

界面上对应：

- 自动
- 低
- 中
- 高
- 极高 xhigh
- 最高 max

不同厂商映射不同：

- OpenAI 使用 `reasoning_effort`
- Claude 使用 thinking budget
- Gemini 2.5 使用 `thinkingBudget`
- Gemini 3 和 4 使用 `thinkingLevel`
- 通义 Qwen3 和部分 GLM 使用各自的 thinking 参数

Gemini 没有 max 档，最高会按 xhigh 处理。不支持的模型不会收到参数，聊天不会因此报错。

## 联网搜索

输入框上方的“联网”按钮，按对话开关。

- Claude：原生 web search 工具
- Gemini：`google_search`
- OpenRouter：模型名自动加 `:online`
- 通义：`enable_search`
- 其他平台会明确提示不支持

切换联网状态会让这一轮缓存重新计算，应用会提醒。

## 缓存优化

缓存能不能命中由厂商服务器决定。客户端做三件事：保持前缀稳定、按厂商加对参数、把命中情况算清楚。

### 厂商适配

- DeepSeek：自动前缀缓存，64 token 起步，解析命中与未命中 token
- OpenAI：带上稳定的 `prompt_cache_key`，提示 1024 token 门槛
- Claude：自动缓存，断点随对话推进，可开 1 小时缓存，门槛按型号区分
- OpenRouter：带 `session_id` 走黏性路由，Claude 自动加缓存标记
- Gemini：隐式缓存，可选固定资料的显式缓存，门槛按型号区分
- Kimi、智谱、通义、硅基流动：自动缓存，保持前缀稳定
- Groq：Kimi K2 系列支持缓存，其他模型明确提示没有
- Ollama、LM Studio：由本地服务端缓存前缀

### 通用措施

- 新对话固定当时的系统提示词和固定资料
- 聊天只追加，不回改历史
- 重新生成只替换最后一条回复
- 换模型、改提示词、回退历史都会提醒缓存重算
- 每条回复显示命中率和节省的 token
- 设置页提供上一轮缓存诊断
- 提示词体检会自动找出会变的内容，并支持一键挪到最后

## 固定资料

设置里的“固定资料”适合放手册、写作规范、常用代码、长期参考文档。

它会接在系统提示词后面，所有新对话共用。固定资料越大越稳定，缓存越容易命中。它和系统提示词一样，只影响新对话，不会破坏老对话的缓存。

## 壁纸与玻璃

设置里的“外观”可以选择：

- 6 张内置渐变壁纸
- 一张自己的图片作为壁纸
- 玻璃透明度，从更透到更实

有壁纸时，卡片和输入框会变成半透明玻璃。深色模式会自动压暗壁纸，保证文字看得清。

## 历史与搜索

- 左侧搜索框，或聊天页右上角放大镜
- 同时搜索标题和每一句话
- 点搜索结果直接跳到那条消息并高亮
- 长按对话可以重命名或删除
- 按今天、昨天、本周、更早分组

## 导出与导入

- 历史记录右上角，或设置里的“数据与备份”
- 导出为 JSON 文件，只含聊天内容，不含 API Key
- 导入只做新增，不覆盖已有对话，重复对话会自动跳过
- 图片也会一起导出和导入

## 隐私

- 聊天记录、图片和 API Key 只存在这台手机
- 没有账号、广告、第三方统计
- 系统云备份已关闭
- 只有你点发送时，应用才会直连你填的 API 地址
- 仓库不包含任何 API Key 或签名密钥

## 从源码构建

需要 JDK 17 和 Android SDK 34。

```bash
./gradlew :app:testDebugUnitTest :app:lintRelease
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

如果不提供 `signing.properties`，release 包是未签名的，文件名是 `app-release-unsigned.apk`。CI 也是这样构建的。

自己本机签名时，请使用仓库外的密钥文件，并通过 `signing.properties` 或环境变量提供密码。不要把密钥提交到 Git。

## CI

`.github/workflows/android.yml` 会在 GitHub Actions 上：

1. 安装 JDK 17 和 Android SDK 34
2. 运行 25 个单元测试
3. 运行 lint
4. 构建未签名 release APK
5. 上传 `AIChat-unsigned` 工件

CI 不接触任何签名密钥。

## 项目结构

- `app/src/main/java/com/kevin233/chat/data`：数据模型、存储、预设、智能体
- `app/src/main/java/com/kevin233/chat/net`：OpenAI、Claude、Gemini 请求和缓存逻辑
- `app/src/main/java/com/kevin233/chat/ui`：Compose 界面
- `app/src/main/java/com/kevin233/chat/util`：token 估算、提示词体检、图片处理
- `app/src/test`：单元测试
- `scripts/sign-local.sh`：本机签名脚本

## 已知限制

- 图片、联网和思考强度都要看具体模型是否支持
- token 数是本地估算，预热进度是近似值
- Gemini 4 不支持显式缓存，只走隐式缓存
- 联网搜索结果以模型返回的文本为准，没有单独做引用卡片
- 应用没有上架应用商店，安装时需要允许未知来源

## 许可证

MIT，见 [LICENSE](LICENSE)。

署名：Kevin233
