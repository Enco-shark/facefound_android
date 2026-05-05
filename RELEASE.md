# FaceFound v2.1.0 发布说明

> 基于 InsightFace + ONNX Runtime 的离线人脸识别 Android 应用

## 版本变更

| 版本 | 日期 | 说明 |
|------|------|------|
| v2.0.0 | 2025-xx-xx | 初始版本，实现图片人脸识别 |
| v2.1.0 | 2026-05-05 | 新增视频识别功能，GUI 优化，性能提升 |

## v2.1.0 新功能

- **视频人脸识别** -- 批量视频帧处理，输出带标注的人脸识别视频（MP4）
- **视频识别 UI** -- 视频选择、参数设置、进度显示、结果预览
- **模板管理优化** -- 使用 LazyColumn 渲染，支持大规模模板列表（万级不卡顿）
- **largeHeap 支持** -- AndroidManifest 添加 largeHeap 配置，堆内存上限提升
- **GUI 全面优化** -- 侧滑菜单品牌头部、选中项动画过渡、卡片圆角统一、状态图标
- **性能优化** -- 消除冗余 bitmap 拷贝、减少上下文切换、批量 UI 状态更新
- **权限回调保护** -- Activity 权限结果 NPE 容错处理

## 技术栈

| 组件 | 技术 |
|------|------|
| 推理引擎 | ONNX Runtime 1.17.0 |
| 检测模型 | SCRFD det_10g (640x640 -> bbox + 5kps) |
| 识别模型 | ArcFace w600k_r50 (112x112 -> 512-dim) |
| UI 框架 | Jetpack Compose + Material 3 |
| 开发语言 | Kotlin |
| 最低 SDK | Android 7.0 (API 24) |
| 目标 SDK | Android 14 (API 34) |

## 系统要求

| 项目 | 要求 |
|------|------|
| Android 版本 | 7.0 (API 24) 及以上 |
| CPU 架构 | arm64-v8a / armeabi-v7a / x86_64 |
| 可用存储 | ~500MB（模型文件 + 应用数据） |
| 可用内存 | 建议 4GB+ RAM |

## 安装

1. 下载 `app-debug.apk`
2. 传输到 Android 手机
3. 开启"允许安装未知来源应用"
4. 点击安装

## 使用步骤

1. 启动应用，等待模型加载完成（约 1-2 秒）
2. 点击预览区域选择待识别图片，或进入"视频识别"页面选择视频
3. 点击"加载模板"导入 .npz 模板文件（首次使用必须导入）
4. 调节识别阈值（默认 0.45，降低阈值可提高召回率）
5. 点击"开始识别"

## 模板文件

模板为 NumPy .npz 格式，包含 `names` 和 `embeddings` 两个数组。生成方式：

```python
import numpy as np
import insightface

app = insightface.app.FaceAnalysis(name='buffalo_l')
app.prepare(ctx_id=0)

names = []
embeddings = []
for img_path, person_name in image_list:
    img = insightface.app.read_image(img_path)
    faces = app.get(img)
    if len(faces) > 0:
        names.append(person_name)
        embeddings.append(faces[0].normed_embedding)

np.savez("templates.npz",
         names=np.array(names),
         embeddings=np.array(embeddings, dtype=np.float32))
```

将生成的 `templates.npz` 传输到手机，通过应用内"加载模板"导入。

## 性能参考

| 设备 | 检测 | 单人脸识别 | 端到端(单人脸) |
|------|------|-----------|---------------|
| 骁龙 8 Gen 3 | ~30ms | ~20ms | ~50ms |
| 骁龙 8 Gen 2 | ~50ms | ~30ms | ~80ms |
| 骁龙 7 Gen 1 | ~100ms | ~60ms | ~160ms |

## 已知问题

- 模型文件需手动放置到 `app/src/main/assets/` 目录后才能构建
- 模板文件中的嵌入向量必须为 512 维 float32
- 大图片（>4096px）会自动降采样以节省内存
- 相似度低于阈值的人脸显示为 UNKNOWN
- 视频编码功能依赖设备硬件编码器支持（H.264）

## 变更日志

### 新增

- VideoProcessor: 视频帧提取、批量人脸识别、NV12 编码、H.264 输出
- VideoScreen: 视频选择、参数设置、进度显示、帧结果预览
- lazy list 渲染模板/历史列表（替代 forEach）
- largeHeap 堆内存支持
- 权限回调 try-catch 保护

### 修复

- encodeToVideo 的 EOS 误发 bug（仅输出 2 帧）
- drainEncoder 超时丢帧问题（timeout=0）
- compile warning: 未使用变量（templateList, appContext, halfW, imgHeight）
- NpzParser 多余 null 安全调用

### 优化

- 消除 processVideoFrames 中的 withContext 冗余上下文切换
- 消除 drawVideoResults 的 bitmap 重复拷贝
- encodeToVideo 复用 IntArray 像素缓冲区
- convertARGBToNV12 算法优化（while + 位运算 + 预计算）
- ViewModel 批量 UI 状态更新（每 5 帧一次）
