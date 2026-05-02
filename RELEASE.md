# FaceFound v2.0.0

> 基于 InsightFace + ONNX Runtime 的离线人脸识别 Android 应用

## 新功能

- **人脸检测** — SCRFD (det_10g) 多尺度锚点检测，输出边界框 + 5 点关键点
- **人脸对齐** — 5 点最小二乘相似变换，对齐到 ArcFace 标准 112×112 模板
- **人脸识别** — ArcFace (w600k_r50) 提取 512 维嵌入向量，余弦相似度匹配
- **模板管理** — 支持 .npz 格式模板导入，持久化存储，查看/重命名/删除
- **识别历史** — 自动保存识别时间、结果和耗时
- **结果保存** — 识别结果图片保存到系统相册
- **主题切换** — 深色/浅色主题
- **离线运行** — 所有推理在设备端完成，无需网络

## 系统要求

| 项目 | 要求 |
|------|------|
| Android 版本 | 7.0 (API 24) 及以上 |
| CPU 架构 | arm64-v8a / armeabi-v7a / x86_64 |
| 可用存储 | ~300MB（模型文件 + 应用数据） |
| 可用内存 | 建议 4GB+ RAM |

## 安装

1. 下载 `app-debug.apk`
2. 传输到 Android 手机
3. 开启「允许安装未知来源应用」
4. 点击安装

## 使用步骤

1. 启动应用，等待模型加载完成（约 1-2 秒）
2. 点击预览区域选择待识别图片
3. 点击「加载模板」导入 .npz 模板文件（首次使用必须导入）
4. 调节识别阈值（默认 0.45，降低阈值可提高召回率）
5. 点击「开始识别」

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

将生成的 `templates.npz` 传输到手机，通过应用内「加载模板」导入。

## 性能参考

| 设备 | 检测 | 单人脸识别 | 端到端 |
|------|------|-----------|--------|
| 骁龙 8 Gen 3 | ~30ms | ~20ms | ~50ms |
| 骁龙 8 Gen 2 | ~50ms | ~30ms | ~80ms |
| 骁龙 7 Gen 1 | ~100ms | ~60ms | ~160ms |

## 技术栈

| 组件 | 技术 |
|------|------|
| 推理引擎 | ONNX Runtime 1.17.0 |
| 检测模型 | SCRFD det_10g (640×640 → bbox + 5kps) |
| 识别模型 | ArcFace w600k_r50 (112×112 → 512-dim) |
| UI 框架 | Jetpack Compose + Material 3 |
| 开发语言 | Kotlin |
| 最低 SDK | Android 7.0 (API 24) |
| 目标 SDK | Android 14 (API 34) |

## 已知问题

- 模型文件需手动放置到 `app/src/main/assets/` 目录后才能构建
- 模板文件中的嵌入向量必须为 512 维 float32
- 大图片（>4096px）会自动降采样以节省内存
- 相似度低于阈值的人脸显示为 UNKNOWN

## 文件校验

| 文件 | 说明 |
|------|------|
| app-debug.apk | Debug 签名，可直接安装 |
