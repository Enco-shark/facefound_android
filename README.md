# FaceFound - 人脸识别 Android 应用

基于 InsightFace (buffalo_l) 和 ONNX Runtime Mobile 的高性能离线人脸识别 Android 应用。

---

## 功能特性

- **实时摄像头识别** - CameraX 实时预览，逐帧检测+识别，实时显示人脸框和姓名标注
- **人脸检测** - SCRFD (det_10g) 多尺度锚点检测，输出边界框 + 5 点关键点
- **人脸对齐** - 5 点最小二乘相似变换，对齐到 ArcFace 标准 112x112 模板
- **人脸识别** - ArcFace (w600k_r50) 提取 512 维嵌入向量，余弦相似度匹配
- **多线程并行处理** - 多张人脸并行检测+识别，Semaphore 控制 ONNX 推理并发
- **视频识别** - 批量视频帧处理，输出带标注的人脸识别视频
- **阈值独立调节** - 检测置信度阈值和识别相似度阈值独立可调
- **模板管理** - 支持 .npz 格式模板导入，持久化存储，查看/重命名/删除
- **识别历史** - 自动保存识别时间、结果和耗时
- **结果保存** - 识别结果图片保存到系统相册
- **主题切换** - 深色/浅色主题
- **现代化 UI** - Jetpack Compose + Material 3
- **离线运行** - 所有推理在设备端完成，无需网络
- **关于页面** - 应用信息、开发人员、核心依赖致谢、许可证、项目链接

---

## 依赖需求

### 开发环境

| 项目 | 版本 | 说明 |
|------|------|------|
| Android Studio | Giraffe (2023.1.1)+ | 推荐 Hedgehog 或更新 |
| JDK | 17 | 必须，不支持 JDK 8/11 |
| Gradle | 8.2 | 项目自带 wrapper |
| Android Gradle Plugin | 8.2.2 | 构建插件 |
| Kotlin | 1.9.20 | 编译器版本 |
| Android SDK | compileSdk 34 | 编译目标 |
| minSdk | 24 | 最低支持 Android 7.0 |
| targetSdk | 34 | 目标 Android 14 |

### 运行时依赖

| 库 | 版本 | 用途 |
|----|------|------|
| ONNX Runtime | 1.17.0 | 模型推理引擎（核心） |
| CameraX | 1.3.1 | 实时摄像头预览与帧分析 |
| Jetpack Compose | BOM 2024.02.00 | UI 框架 |
| Material 3 | (BOM 管理) | 设计系统 |
| Navigation Compose | 2.7.7 | 页面导航 |
| Coil | 2.5.0 | 图片加载 |
| Kotlin Coroutines | 1.7.3 | 异步处理 |
| Lifecycle Runtime KTX | 2.7.0 | 生命周期管理 |
| Activity Compose | 1.8.2 | Compose 集成 |

### 模型文件（需手动放置）

| 文件 | 来源 | 大小 | 说明 |
|------|------|------|------|
| det_10g.onnx | InsightFace buffalo_l | ~16MB | SCRFD 人脸检测模型 |
| w600k_r50.onnx | InsightFace buffalo_l | ~166MB | ArcFace 人脸识别模型 |

模型文件需放置于 `app/src/main/assets/` 目录。

---

## 构建教程

### 1. 安装开发环境

1. 安装 JDK 17
   - Windows: 下载 [Adoptium JDK 17](https://adoptium.net/)，安装后设置 `JAVA_HOME`
   - 验证: `java -version` 输出 `17.x.x`

2. 安装 Android Studio
   - 下载 [Android Studio](https://developer.android.com/studio)
   - 安装时勾选 Android SDK，确保 SDK Platform 34 已安装
   - 配置 SDK Manager: 勾选 `Android 14 (API 34)` 和 `NDK`

3. 配置环境变量
   ```
   ANDROID_HOME = C:\Users\<用户名>\AppData\Local\Android\Sdk
   Path 添加: %ANDROID_HOME%\platform-tools
   ```

### 2. 获取模型文件

从 InsightFace 获取 buffalo_l 模型包:

```bash
# Python 方式
pip install insightface
python -c "import insightface; model = insightface.app.FaceAnalysis(name='buffalo_l'); model.prepare(ctx_id=-1)"
# 模型下载到 ~/.insightface/models/buffalo_l/
```

将模型复制到项目:

```bash
# Windows
xcopy /Y "%USERPROFILE%\.insightface\models\buffalo_l\det_10g.onnx" "app\src\main\assets\"
xcopy /Y "%USERPROFILE%\.insightface\models\buffalo_l\w600k_r50.onnx" "app\src\main\assets\"

# Linux/Mac
cp ~/.insightface/models/buffalo_l/det_10g.onnx app/src/main/assets/
cp ~/.insightface/models/buffalo_l/w600k_r50.onnx app/src/main/assets/
```

### 3. 导入与同步

1. 打开 Android Studio -> `Open an Existing Project` -> 选择 `android_project` 目录
2. 等待 Gradle 同步完成（首次可能需要下载依赖，约 5-10 分钟）
3. 如同步失败，检查网络或配置国内镜像

### 4. 连接设备

1. 手机开启 `开发者选项` -> `USB 调试`
2. USB 连接电脑，授权调试
3. Android Studio 顶部设备列表选择你的手机

### 5. 运行与构建

**Android Studio 运行**: 点击 Run 按钮或 `Shift+F10`

**命令行构建**:

```bash
# Windows PowerShell
.\gradlew.bat assembleDebug

# Linux/Mac
chmod +x gradlew
./gradlew assembleDebug
```

**构建输出**: `app/build/outputs/apk/debug/app-debug.apk`

**Release 构建**:

```bash
.\gradlew.bat assembleRelease
```

输出: `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## 项目结构

```
android_project/
├── app/
│   ├── build.gradle.kts                    # 应用构建配置（依赖、SDK版本、Compose）
│   ├── proguard-rules.pro                  # ProGuard 混淆规则
│   └── src/main/
│       ├── AndroidManifest.xml             # 应用清单（权限、Activity声明）
│       ├── assets/                         # 模型文件目录
│       │   ├── det_10g.onnx                #   人脸检测模型 (~16MB)
│       │   └── w600k_r50.onnx              #   人脸识别模型 (~166MB)
│       ├── java/com/Enco/facefound/
│       │   ├── MainActivity.kt             # 主 Activity（Compose 入口）
│       │   ├── FaceRecognitionApp.kt       # Application 类
│       │   ├── ml/
│       │   │   └── OnnxFaceRecognition.kt  # 核心推理引擎
│       │   ├── ui/
│       │   │   ├── screens/
│       │   │   │   ├── MainScreen.kt       #   Compose 主界面
│       │   │   │   ├── CameraScreen.kt     #   实时摄像头识别界面
│       │   │   │   ├── VideoScreen.kt      #   视频识别界面
│       │   │   │   └── AboutScreen.kt      #   关于屏幕
│       │   │   ├── theme/
│       │   │   │   ├── Color.kt            #   颜色定义
│       │   │   │   ├── Theme.kt            #   主题配置
│       │   │   │   └── Type.kt             #   字体排版
│       │   │   └── viewmodel/
│       │   │       └── FaceRecognitionViewModel.kt  # 视图模型
│       │   ├── util/
│       │   │   ├── NpzParser.kt            #   NPZ 模板解析器
│       │   │   └── TemplateRepository.kt   #   模板持久化存储
│       │   └── video/
│       │       └── VideoProcessor.kt       #   视频帧处理与编码器
│       └── res/                            # 资源文件（图标、字符串、主题）
├── build.gradle.kts                        # 项目级构建配置（AGP/Kotlin版本）
├── settings.gradle.kts                     # 项目设置（模块声明）
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties       # Gradle 版本配置 (8.2)
└── README.md
```

### 核心文件说明

| 文件 | 职责 |
|------|------|
| OnnxFaceRecognition.kt | ONNX 模型加载、人脸检测、对齐、特征提取、识别匹配、并行处理 |
| VideoProcessor.kt | 视频帧提取、批量人脸识别、结果绘制、H.264 编码输出 |
| FaceRecognitionViewModel.kt | UI 状态管理、识别/视频/摄像头流程编排、模板管理 |
| CameraScreen.kt | CameraX 实时预览、逐帧分析、人脸叠加层绘制 |
| NpzParser.kt | NPZ/ZIP 解析、NPY header 解析、Unicode 名字解析、嵌入归一化 |
| TemplateRepository.kt | 模板二进制序列化、原子写入、索引管理 |
| MainScreen.kt | Compose UI 布局、导航抽屉、设置页面 |
| AboutScreen.kt | 关于页面，包含应用信息、开发人员、依赖致谢、许可证 |

---

## 原理

### 整体流程

```
输入图片/视频帧/摄像头帧
   |
   v
+-------------------------+
|  人脸检测 (det_10g.onnx) |
|  SCRFD 多尺度锚点检测     |
|  输入: 640x640 RGB       |
|  输出: 边界框 + 5关键点   |
|  后处理: 阈值过滤 + NMS   |
+-----------+-------------+
            | 多张人脸并行
            v
+-------------------------+
|  人脸对齐 (alignFace)    |  <-- 并行: 纯CPU，无并发限制
|  5点最小二乘相似变换      |
|  源关键点 -> ArcFace标准点|
|  输出: 112x112 对齐人脸   |
+-----------+-------------+
            |
            v
+-------------------------+
|  特征提取 (w600k_r50)    |  <-- 并行: Semaphore限制并发数
|  ArcFace 识别模型         |
|  输入: 112x112 RGB       |
|  输出: 512维嵌入向量      |
|  后处理: L2归一化         |
+-----------+-------------+
            |
            v
+-------------------------+
|  身份匹配 (recognizeFace) |
|  余弦相似度比对           |
|  与模板库逐个比较          |
|  阈值判断 -> 姓名/UNKNOWN  |
+-------------------------+

并行策略: recognizeFacesParallel()
- 多张人脸使用 async 并行处理
- 对齐(CPU)完全并行，ONNX推理受Semaphore保护
- 默认并发上限: 2 (可通过maxConcurrency调整)

实时摄像头: CameraX Pipeline
- CameraX PreviewView 显示摄像头画面
- ImageAnalysis 逐帧分析 (STRATEGY_KEEP_ONLY_LATEST)
- AtomicBoolean 节流，避免帧积压
- Canvas 叠加层实时绘制边界框和姓名
```

### 人脸检测 (SCRFD)

det_10g.onnx 是 InsightFace 的 SCRFD (Sample and Computation Redistribution for Face Detection) 模型:

- **输入**: `[1, 3, 640, 640]` -- 单张 640x640 RGB 图片
- **预处理**: `pixel = (channel - 127.5) / 128.0`，BGR 通道顺序
- **输出**: 9 个张量，按类型分组:
  - 3 个 score 张量: `[12800,1]`, `[3200,1]`, `[800,1]` -- 步长 8/16/32 的置信度
  - 3 个 bbox 张量: `[12800,4]`, `[3200,4]`, `[800,4]` -- 边界框偏移
  - 3 个 kps 张量: `[12800,10]`, `[3200,10]`, `[800,10]` -- 5个关键点坐标
- **每个位置 2 个锚点** (anchorsPerPos=2)
- **后处理**: 置信度阈值过滤 -> 解码边界框/关键点 -> NMS 去重

### 人脸对齐 (Similarity Transform)

将检测到的人脸关键点对齐到 ArcFace 标准模板坐标:

```
ArcFace 标准目标点 (112x112):
  左眼:  (38.29, 51.70)
  右眼:  (73.53, 51.50)
  鼻尖:  (56.03, 71.74)
  左嘴角: (41.55, 92.37)
  右嘴角: (70.73, 92.20)
```

使用 5 个点对做**最小二乘相似变换**估计，求解 4 参数变换 `[a, b, tx, ty]`:

```
x' = a*x - b*y + tx
y' = b*x + a*y + ty
```

其中 `a = scale*cos(theta)`, `b = scale*sin(theta)`，通过构建正规方程 `A^T A * p = A^T b` 用高斯消元法求解。这比仅用双眼 2 点的变换更精确，能同时校正鼻尖和嘴角位置。

### 人脸识别 (ArcFace)

w600k_r50.onnx 是 InsightFace 的 ArcFace 识别模型:

- **输入**: `[1, 3, 112, 112]` -- 对齐后的 112x112 RGB 人脸
- **预处理**: `pixel = (channel - 127.5) / 128.0`，BGR 通道顺序
- **输出**: `[1, 512]` -- 512 维嵌入向量
- **后处理**: L2 归一化，使向量模长为 1

### 身份匹配

使用**余弦相似度**比较实时嵌入与模板嵌入:

```
similarity = (a . b) / (||a|| * ||b||)
```

由于嵌入已 L2 归一化，余弦相似度等价于点积，范围 [-1, 1]。阈值 0.30 为默认值，可调节:
- 高阈值 (0.5+): 更严格，减少误识但可能漏识
- 低阈值 (0.3): 更宽松，提高召回但可能误识

### NPZ 模板解析

NPZ 文件本质是 ZIP 压缩包，包含多个 `.npy` 数组文件:

```
templates.npz (ZIP)
  |-- names.npy        -- dtype='<U9', shape=(N,) -- N 个人名，每个最多9个UTF-32字符
  +-- embeddings.npy   -- dtype='<f4', shape=(N, 512) -- N 个512维float32嵌入向量
```

解析流程:
1. 解压 ZIP，读取 `names.npy` 和 `embeddings.npy`
2. 名字解析: 根据 dtype `<U9` 按 UTF-32LE 编码逐元素读取，到 `\u0000` 截止
3. 嵌入解析: 按 NPY header 中的 shape 拆分为 N 个 512 维向量
4. L2 归一化: 对每个嵌入向量归一化，与实时提取的嵌入保持一致
5. 配对: 重复名字添加索引后缀，确保 Map 键唯一

---

## 使用说明

### 操作流程

1. 启动应用 -> 自动加载模型和已保存模板
2. 选择图片 -> 点击预览区域
3. 导入模板 -> 点击"加载模板"选择 .npz 文件（首次使用）
4. 调节阈值 -> 拖动滑块调节检测阈值和识别阈值
5. 开始识别 -> 点击按钮，查看结果

### 实时摄像头识别流程

1. 侧滑菜单 -> 进入"实时识别"页面
2. 授予权限 -> 首次使用需授予摄像头权限
3. 对准人脸 -> 将摄像头对准人脸，自动检测+识别
4. 查看结果 -> 实时显示人脸框、姓名和置信度

### 视频识别流程

1. 侧滑菜单 -> 进入"视频识别"页面
2. 选择视频 -> 点击"选择视频"按钮
3. 设置参数 -> 调整相似度阈值、检测阈值和采样间隔
4. 开始处理 -> 点击"开始识别"，等待处理完成
5. 保存结果 -> 点击"保存视频"，输出文件将保存到系统相册

### 模板文件格式

NPZ 格式，包含 `names` 和 `embeddings` 两个数组:

```python
import numpy as np
import insightface

# 方式1: 从 InsightFace 提取
app = insightface.app.FaceAnalysis(name='buffalo_l')
app.prepare(ctx_id=0)

names = []
embeddings = []
for img_path in image_list:
    img = insightface.app.read_image(img_path)
    faces = app.get(img)
    if len(faces) > 0:
        names.append(person_name)
        embeddings.append(faces[0].normed_embedding)

np.savez("templates.npz",
         names=np.array(names),
         embeddings=np.array(embeddings, dtype=np.float32))

# 方式2: 手动构建
names = np.array(["张三", "李四"])
embeddings = np.array([[0.1, 0.2, ...], [0.3, 0.4, ...]], dtype=np.float32)
np.savez("templates.npz", names=names, embeddings=embeddings)
```

---

## 故障排除

### Gradle 同步失败

1. 检查 JDK 版本是否为 17: `java -version`
2. 检查网络连接
3. 配置国内镜像: 修改 `settings.gradle.kts` 添加阿里云镜像
4. File -> Invalidate Caches / Restart

### 模型加载失败

1. 确认 `app/src/main/assets/` 下有 `det_10g.onnx` 和 `w600k_r50.onnx`
2. 确认文件完整（检测模型 ~16MB，识别模型 ~166MB）
3. Clean Project 后重新构建

### 识别全部返回 UNKNOWN

1. 确认模板文件格式正确（.npz，包含 names + embeddings）
2. 确认 embeddings 维度为 512
3. 尝试降低阈值（0.25-0.30）
4. 查看日志中的相似度分数判断是否对齐问题

### 应用闪退

1. 检查设备架构（支持 arm64-v8a, armeabi-v7a, x86_64）
2. 检查可用内存（模型加载需要 ~300MB）
3. 查看 logcat 日志定位异常

### 摄像头无法使用

1. 确认已授予摄像头权限（设置 → 应用 → FaceFound → 权限）
2. 确认设备有摄像头硬件
3. 如摄像头被其他应用占用，关闭后重试
4. 查看 logcat 中 `CameraScreen` 标签的日志

---

## 性能参考

### 单帧识别

| 设备 | 检测 | 单人脸识别 | 端到端(单人脸) |
|------|------|-----------|---------------|
| 骁龙 8 Gen 3 | ~30ms | ~20ms | ~50ms |
| 骁龙 8 Gen 2 | ~50ms | ~30ms | ~80ms |
| 骁龙 7 Gen 1 | ~100ms | ~60ms | ~160ms |

### 多人脸并行处理

并行处理对多人脸场景有显著加速效果（2+ 张人脸时）：
- 对齐阶段完全并行（纯 CPU，无并发限制）
- ONNX 推理通过 Semaphore(2) 限制并发，平衡速度与内存
- 模板匹配（余弦相似度）为纯 CPU 计算，完全并行

### 实时摄像头识别

- 分析帧率: ~5fps（受 ONNX 推理耗时影响）
- 显示帧率: 30fps（CameraX 预览独立于分析）
- 坐标映射: 分析分辨率(640x480) → 显示分辨率，自动缩放

---

## 代码规范

本项目遵循 Android Kotlin 代码注释规范:

1. **逐行注释**: 所有逻辑代码行均添加中文注释，说明变量用途、函数目的、参数含义、返回值和异常边界
2. **生命周期注释**: Activity 生命周期、页面导航、网络请求、UI 适配和权限逻辑均逐行标注
3. **Jetpack 规范**: ViewModel、Activity、Fragment 流程逐行注释
4. **异常标注**: 异常捕获、内存泄漏和线程安全逻辑需标注风险和原因
5. **避免冗余**: 注释需准确解释代码功能和业务逻辑，避免无意义的重复说明

---

## 许可证

本项目基于 InsightFace 和 ONNX Runtime 构建，遵循各自的开源协议。
