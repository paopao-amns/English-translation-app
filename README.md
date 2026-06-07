# English Learner — 英语学习助手 (Android)

基于 Chrome 扩展 [English-translation](https://github.com/paopao-amns/English-translation) 开发的 Android 版本，新增 OCR 图片识别、两种阅读模式、生词本等功能。

## 功能特性

### 核心功能（继承自 Chrome 扩展）

| 功能 | 说明 |
|------|------|
| 🔍 **单词查询** | 输入或点击单词，获取词性、中文释义、英文例句（含中文翻译） |
| 📝 **句子成分分析** | 输入或选择句子，AI 分析语法结构：句子类型、主谓宾、修饰成分、从句、中文翻译、构造解释 |
| 💬 **交互式问答** | 在句子分析后追问语法问题，对话记录最多保存 10 条 |

### Android 独有功能

| 功能 | 说明 |
|------|------|
| 📷 **图片 OCR 识别** | 拍照或从相册选取，Google ML Kit 端侧识别英文文本（离线可用） |
| 📖 **两种阅读模式** | **按需查询**：点击单词/选择句子才调用 API；**预加载**：提前分析全文并缓存到本地 |
| ⚡ **三种预加载策略** | 逐个分析 / 批量分析 / 智能混合（短文章批量，长文章分段） |
| 🔊 **单词发音** | Android TTS 引擎，点击即可听到单词朗读 |
| 📔 **生词本** | 收藏单词，支持导出 CSV / Anki 格式 |
| 🌙 **深色模式** | 跟随系统或手动切换 |
| 🔌 **多供应商支持** | OpenAI / DeepSeek / 通义千问 / 智谱 GLM / Kimi / 小米 MiMo / 硅基流动 / 自定义 |

## 安装

### 从源码构建

1. 克隆项目：
```bash
git clone https://github.com/paopao-amns/English-translation.git
cd EnglishLearner
```

2. 使用 Android Studio (Hedgehog 2023.1+) 打开项目

3. 等待 Gradle 同步完成后，连接 Android 设备或启动模拟器 (API 26+)

4. 点击 **Run** 按钮

### 要求
- Android 8.0 (API 26) 或更高版本
- 相机权限（用于 OCR 扫描）
- 网络权限（用于 API 调用）

## 配置

首次使用需要在「设置」中配置 API：

1. 点击右上角齿轮图标进入设置
2. 选择 AI 供应商（OpenAI、DeepSeek 等 7 个预设，或自定义）
3. 填入你的 API Key
4. （可选）修改模型名称
5. 点击「测试连接」确认配置正确
6. 点击「保存设置」

### 获取 API Key

| 供应商 | 获取地址 |
|--------|---------|
| OpenAI | https://platform.openai.com/api-keys |
| DeepSeek | https://platform.deepseek.com/api_keys |
| 通义千问 | https://dashscope.console.aliyun.com/apiKey |
| 智谱 GLM | https://open.bigmodel.cn/usercenter/apikeys |
| Kimi | https://platform.moonshot.cn/console/api-keys |
| 硅基流动 | https://siliconflow.cn/ |

## 使用指南

### 扫描文章
1. 首页点击「扫描文章」
2. 选择「拍照」或「从相册选择」
3. 等待 OCR 识别完成，可编辑修正识别结果
4. 点击右上角「完成」进入阅读模式

### 阅读模式 — 按需查询（默认）
- **点击单词**：底部弹出单词释义卡片（含发音按钮）
- **选择句子**：选中文本后点击底部「分析句子」按钮
- **手动查词**：底部输入框可以直接输入单词查询

### 阅读模式 — 预加载
1. 点击顶部模式切换按钮切换到「预加载」
2. 点击底部「预加载」按钮
3. 等待 AI 分析全文（预加载的词句会高亮显示）
4. 点击高亮词句即时查看结果（无需等待 AI 响应）
5. 退出后重新进入，缓存数据依然可用

### 句子分析
- 分析结果包含：句子类型、基本成分、修饰成分、从句分析、中文翻译、构造解释
- 可展开/折叠各个分析模块
- 点击「追问」可以针对该句子提问

### 追问
- 在分析结果页点击「追问」
- 输入语法问题（如「为什么这里用过去时而不是现在完成时？」）
- AI 结合原句上下文给出针对性解答
- 对话记录自动保存（最多 10 条），点击「清除」可删除

### 生词本
- 查词时点击书签图标收藏
- 在首页点击「生词本」查看和管理
- 支持导出为 CSV（Excel 兼容）或 Anki 格式

## 项目结构

```
EnglishLearner/
├── app/src/main/java/com/paopao/englearn/
│   ├── EngLearnApp.kt                 # Application + DI
│   ├── MainActivity.kt                # Single-activity host
│   ├── data/
│   │   ├── remote/                    # API (Retrofit, models, prompts)
│   │   ├── local/                     # Room (entities, DAOs, database)
│   │   ├── preferences/               # DataStore (settings)
│   │   └── repository/                # Business logic
│   ├── domain/model/                  # Domain models
│   ├── ui/
│   │   ├── navigation/                # Compose Navigation
│   │   ├── theme/                     # Material 3 theme
│   │   ├── components/                # Reusable composables
│   │   ├── home/                      # Home screen
│   │   ├── ocr/                       # OCR, reading, tappable text
│   │   ├── lookup/                    # Word lookup
│   │   ├── analysis/                  # Sentence analysis
│   │   ├── chat/                      # Q&A chat
│   │   └── settings/                  # Settings, vocabulary
│   └── util/                          # Utilities
├── app/src/main/res/                  # Resources
└── README.md
```

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Repository |
| 网络 | Retrofit + OkHttp + kotlinx.serialization |
| 本地存储 | Room + DataStore |
| OCR | Google ML Kit Text Recognition V2 |
| 相机 | CameraX |
| 导航 | Compose Navigation |
| 最低 SDK | API 26 (Android 8.0) |

## 与 Chrome 扩展的关系

本项目的 API 调用格式、System Prompt、供应商预设与 [Chrome 扩展版本](https://github.com/paopao-amns/English-translation) 完全兼容。如果你已经在 Chrome 扩展中配置了 API Key，可以在 Android 端使用相同的 Key。

## License

MIT License
