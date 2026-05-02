# FaceFound 模型文件放置说明

请将以下 InsightFace buffalo_l 模型文件放置在此目录：

1. det_10g.onnx        (人脸检测模型，约170MB)
2. w600k_r50.onnx      (人脸识别模型，约65MB)

## 获取模型

### 方式1: 从官方下载
- 访问: https://github.com/deepinsight/insightface/releases
- 下载 buffalo_l.zip
- 解压后复制上述两个文件到 assets 目录

### 方式2: 使用桌面版的模型
如果你已经有桌面版的 buffalo_l 文件夹，直接从里面复制上述文件

## 注意事项

- 文件名必须完全匹配（区分大小写）
- 确保文件完整性，不要损坏
- 模型文件较大，首次构建APK时会比较慢
