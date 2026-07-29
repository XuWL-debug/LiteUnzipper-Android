# LiteUnzipper Android - 更新日志

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
- minSdk: 30 (Android 11)
- targetSdk: 34
- versionCode: 2
- versionName: 1.1.0
- Release APK: 6.15 MB
- Debug APK: 17.15 MB
