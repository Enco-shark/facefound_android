import os

# 需要更新的目录
base_dir = "D:/data/Desktop/android_project/app/src/main/java/com/Enco/facerecognition"

# 遍历所有.kt文件
for root, dirs, files in os.walk(base_dir):
    for file in files:
        if file.endswith(".kt"):
            file_path = os.path.join(root, file)
            
            # 读取文件（UTF-8编码）
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 执行替换 - 替换所有 com.Enco.facefound 出现的地方
            if "com.Enco.facefound" in content:
                print(f"Fixing: {file_path}")
                content = content.replace("com.Enco.facefound", "com.Enco.facerecognition")
                
                # 写回文件（UTF-8编码）
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(content)

print("✅ 所有完全限定类型引用已更新")
