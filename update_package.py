import os

# 需要更新的目录
base_dir = "D:/data/Desktop/android_project/app/src/main/java/com/Enco/facerecognition"

# 遍历所有.kt文件
for root, dirs, files in os.walk(base_dir):
    for file in files:
        if file.endswith(".kt"):
            file_path = os.path.join(root, file)
            print(f"Processing: {file_path}")
            
            # 读取文件（UTF-8编码）
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 执行替换
            # 1. 替换package声明
            content = content.replace("package com.Enco.facefound", "package com.Enco.facerecognition")
            
            # 2. 替换import语句
            content = content.replace("import com.Enco.facefound", "import com.Enco.facerecognition")
            
            # 3. 替换硬编码字符串
            content = content.replace("facefound_settings", "facerecognition_settings")
            content = content.replace("FaceFound_", "FaceRecognition_")
            content = content.replace("Pictures/FaceFound", "Pictures/FaceRecognition")
            content = content.replace("Movies/FaceFound", "Movies/FaceRecognition")
            
            # 写回文件（UTF-8编码）
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)

print("✅ 所有Kotlin文件已更新")
