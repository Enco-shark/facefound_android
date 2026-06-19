import os
import glob

# 需要更新的文档文件
docs = [
    "D:/data/Desktop/android_project/README.md",
    "D:/data/Desktop/android_project/AGENTS.md",
    "D:/data/Desktop/android_project/CLAUDE.md",
    "D:/data/Desktop/android_project/ONNX_SETUP_GUIDE.md",
    "D:/data/Desktop/android_project/PERFORMANCE_ANALYSIS_REPORT.md"
]

for doc in docs:
    if os.path.exists(doc):
        print(f"Processing: {doc}")
        
        # 读取文件（UTF-8编码）
        with open(doc, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 执行替换
        content = content.replace("com.Enco.facefound", "com.Enco.facerecognition")
        content = content.replace("FaceFound", "FaceRecognition")
        content = content.replace("facefound", "facerecognition")
        
        # 写回文件（UTF-8编码）
        with open(doc, 'w', encoding='utf-8') as f:
            f.write(content)

print("✅ 所有文档文件已更新")
