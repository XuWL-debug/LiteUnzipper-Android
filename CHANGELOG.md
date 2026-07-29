# LiteUnzipper Android - 更新日志

## 版本分支归档

| 版本 | 分支 | 描述 | APK 大小 |
|------|------|------|----------|
| v1.0.0 | `release/v1.0.0` | 初始 Material Design 版本 | ~14 MB |
| v1.1.0 | `release/v1.1.0` | 增强功能版本（暗色模式/线程/密码/批量） | 6.15 MB |
| v1.1.0-glass | `release/v1.1.0-glass` | 苹果玻璃设计版本（Glassmorphism + 动画） | 6.19 MB |

---

## v1.1.0-glass - 苹果玻璃设计版本 (Latest)

**发布日期**: 2026-07-29

### 设计语言
- Apple Liquid Glass 设计系统
- 极光渐变背景 (bg_aurora_gradient.xml)
- 玻璃态卡片效果（79.6% 透明度 + 0.5dp 高亮边框）
- SF Pro 风格排版（32sp 大标题、17sp 标题、15sp 正文）
- iOS 色彩体系（#007AFF 主蓝色、#FF3B30 iOS 红色）
- 24dp 圆角玻璃卡片 + Squircle 图标背景
- 沉浸式边缘到边缘显示 (setDecorFitsSystemWindows(false))

### 动画系统
- 自定义插值器（ease_out_back、ease_out_smooth、spring_bounce）
- 进场动画（item_enter、layout_items_enter、topbar_enter、dock_enter）
- 按压回弹动画（FileListAdapter & MainActivity）
- 顶栏 + Dock + 文件列表交错进场序列

### 保留功能
- 暗色模式切换（同步系统）
- 解压线程数选择（1-4线程）
- 全格式支持（ZIP / RAR / 7z / TAR / GZIP / BZIP2 / XZ / Zstandard + 分卷）
- 密码保护压缩包解压
- 批量解压
- 加密检测 + 密码重试流程
- 详细错误信息 + 取消解压

### 技术规格
- minSdk: 30 (Android 11)
- targetSdk: 34
- versionCode: 2
- versionName: 1.1.0
- Release APK: 6.19 MB
- 签名: v2 签名 (liteunzipper123)

---

## v1.1.0 - 增强功能版本

**发布日期**: 2026-07-29

### 新增功能
- 暗色模式切换（同步系统暗色模式）
- 解压线程数选择（1-4线程，自动识别设备 CPU 核心数）
- 扩展压缩格式支持（ZIP / RAR / 7z / TAR / GZIP / BZIP2 / XZ / Zstandard + 分卷压缩）
- 密码保护压缩包解压（加密检测 + 密码重试流程）
- 批量解压功能
- 详细错误信息展示
- 取消解压按钮

### 优化
- R8 代码压缩 + 资源压缩，APK 从 14 MB 减至 6.13 MB
- Material You 动态配色（Android 12+），Android 11 回退静态色板
- SAF (Storage Access Framework) 文件操作
- 签名 APK（v2 签名验证）

### 技术规格
- versionCode: 2
- versionName: 1.1.0
- Release APK: 6.15 MB
- Debug APK: 17.15 MB

---

## v1.0.0 - 初始 Material Design 版本

**发布日期**: 2026-07-29

### 功能
- 基础解压功能（ZIP / RAR / 7z / TAR / GZIP / BZIP2 / XZ / Zstandard）
- Material You 动态配色（Android 12+），Android 11 回退静态色板
- Storage Access Framework (SAF) 文件操作
- MVVM 架构 + Kotlin 协程
- 轻量化设计，APK 约 14 MB

### 技术规格
- versionCode: 1
- versionName: 1.0.0
