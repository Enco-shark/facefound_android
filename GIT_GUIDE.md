# GitHub 项目上传操作指南

> 适用于 FaceFound Android 项目及一般 Android 项目的 Git 上传流程

---

## 目录

1. [项目初始化](#1-项目初始化)
2. [Git 配置](#2-git-配置)
3. [日常上传流程](#3-日常上传流程)
4. [远程仓库关联](#4-远程仓库关联)
5. [分支管理](#5-分支管理)
6. [常见问题解决](#6-常见问题解决)
7. [Git 命令速查表](#7-git-命令速查表)

---

## 1. 项目初始化

### 1.1 已有项目首次上传

```bash
# 进入项目根目录
cd /path/to/your/project

# 初始化 Git 仓库
git init

# 添加所有文件到暂存区
git add -A

# 提交初始版本
git commit -m "Initial commit"

# 关联远程仓库
git remote add origin https://github.com/username/repo.git

# 推送到 main 分支
git push -u origin main
```

### 1.2 克隆已有仓库

```bash
git clone https://github.com/username/repo.git
cd repo
```

---

## 2. Git 配置

### 2.1 全局配置（只需一次）

```bash
# 设置用户名和邮箱（会显示在提交记录中）
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# 设置默认分支名为 main
git config --global init.defaultBranch main

# 设置换行符自动处理（Windows 推荐）
git config --global core.autocrlf true

# 查看当前配置
git config --list
```

### 2.2 项目级配置

```bash
# 仅对当前项目生效
git config user.name "Project Name"
git config user.email "project@example.com"
```

---

## 3. 日常上传流程

### 3.1 标准四步上传法

```bash
# Step 1: 查看当前状态（确认修改了哪些文件）
git status

# Step 2: 添加文件到暂存区
# 添加所有修改
git add -A

# 或只添加特定文件
git add app/src/main/java/com/example/MainActivity.kt

# Step 3: 提交更改（写清楚提交信息）
git commit -m "feat: 添加视频识别功能

- 新增 VideoProcessor 处理视频帧
- 新增 VideoScreen UI 界面
- 修复编码器 EOS 错误"

# Step 4: 推送到远程仓库
git push origin main
```

### 3.2 提交信息规范

| 前缀 | 含义 | 示例 |
|------|------|------|
| `feat:` | 新功能 | `feat: 添加视频人脸识别` |
| `fix:` | 修复 Bug | `fix: 修复编码器 EOS 错误` |
| `perf:` | 性能优化 | `perf: 减少 bitmap 内存分配` |
| `refactor:` | 重构代码 | `refactor: 优化 NV12 转换算法` |
| `docs:` | 文档更新 | `docs: 更新 README` |
| `style:` | 格式调整 | `style: 统一代码缩进` |
| `chore:` | 杂项 | `chore: 更新依赖版本` |

---

## 4. 远程仓库关联

### 4.1 查看远程仓库

```bash
git remote -v
```

### 4.2 添加/修改远程仓库

```bash
# 添加远程仓库
git remote add origin https://github.com/username/repo.git

# 修改远程仓库地址
git remote set-url origin https://github.com/newuser/newrepo.git

# 删除远程仓库
git remote remove origin
```

### 4.3 使用 SSH（免密码推送）

```bash
# 生成 SSH 密钥（一路回车）
ssh-keygen -t ed25519 -C "your.email@example.com"

# 复制公钥到 GitHub Settings -> SSH Keys
cat ~/.ssh/id_ed25519.pub

# 修改远程地址为 SSH 格式
git remote set-url origin git@github.com:username/repo.git
```

---

## 5. 分支管理

### 5.1 基本分支操作

```bash
# 查看所有分支
git branch -a

# 创建新分支
git branch feature/video

# 切换到分支
git checkout feature/video

# 创建并切换（一步完成）
git checkout -b feature/video

# 合并分支到当前分支
git merge feature/video

# 删除本地分支
git branch -d feature/video

# 删除远程分支
git push origin --delete feature/video
```

### 5.2 推荐分支策略

```
main          生产环境分支（稳定版本）
  |
  +-- dev     开发分支（日常开发）
  |     |
  |     +-- feature/xxx   功能分支
  |     +-- fix/xxx       修复分支
  |
  +-- release/v1.0  发布分支
```

---

## 6. 常见问题解决

### 6.1 推送被拒绝（远程有更新）

```bash
# 先拉取远程更新
git pull origin main

# 如果有冲突，解决后重新提交
git add -A
git commit -m "merge: 解决冲突"
git push origin main
```

### 6.2 撤销未提交的修改

```bash
# 撤销单个文件的修改
git checkout -- filename.kt

# 撤销所有未暂存的修改
git checkout -- .

# 撤销已暂存但未提交的修改
git reset HEAD filename.kt
```

### 6.3 修改最后一次提交

```bash
# 修改提交信息
git commit --amend -m "新的提交信息"

# 添加遗漏的文件到最后一次提交
git add forgotten.kt
git commit --amend --no-edit
```

### 6.4 大文件无法推送

```bash
# 查看大文件
git ls-files | xargs -I{} du -sh {} | sort -rh | head -n 10

# 从 Git 历史中删除大文件（使用 BFG Repo-Cleaner）
java -jar bfg.jar --delete-files *.onnx
```

### 6.5 LF/CRLF 换行符警告

```bash
# Windows 用户设置自动转换
git config --global core.autocrlf true

# 忽略此警告（不推荐）
git config --global core.safecrlf false
```

### 6.6 忘记添加文件到 .gitignore

```bash
# 1. 添加到 .gitignore
echo "*.log" >> .gitignore

# 2. 从 Git 中移除但保留本地文件
git rm --cached app/logcat.txt

# 3. 提交 .gitignore
git add .gitignore
git commit -m "chore: 更新 .gitignore"
```

---

## 7. Git 命令速查表

### 基础命令

| 命令 | 作用 |
|------|------|
| `git init` | 初始化仓库 |
| `git clone <url>` | 克隆远程仓库 |
| `git status` | 查看工作区状态 |
| `git add <file>` | 添加文件到暂存区 |
| `git add -A` | 添加所有变更 |
| `git commit -m "msg"` | 提交更改 |
| `git push` | 推送到远程 |
| `git pull` | 拉取远程更新 |

### 查看历史

| 命令 | 作用 |
|------|------|
| `git log` | 查看提交历史 |
| `git log --oneline` | 简洁历史 |
| `git log --graph` | 图形化分支历史 |
| `git diff` | 查看未暂存的差异 |
| `git diff --cached` | 查看已暂存的差异 |

### 撤销操作

| 命令 | 作用 |
|------|------|
| `git reset HEAD <file>` | 取消暂存 |
| `git checkout -- <file>` | 撤销修改 |
| `git reset --soft HEAD~1` | 撤销最后一次提交（保留修改） |
| `git reset --hard HEAD~1` | 撤销最后一次提交（丢弃修改） |

### 分支操作

| 命令 | 作用 |
|------|------|
| `git branch` | 列出本地分支 |
| `git branch <name>` | 创建分支 |
| `git checkout <name>` | 切换分支 |
| `git merge <name>` | 合并分支 |
| `git branch -d <name>` | 删除分支 |

### 远程操作

| 命令 | 作用 |
|------|------|
| `git remote -v` | 查看远程仓库 |
| `git fetch` | 获取远程更新（不合并） |
| `git pull` | 拉取并合并 |
| `git push -u origin main` | 首次推送并关联分支 |

---

## 附录：FaceFound 项目专属配置

### 项目 .gitignore 建议

```gitignore
# Gradle
.gradle/
build/
app/build/
app/debug/
app/release/

# IDE
.idea/
*.iml
*.ipr
*.iws

# 本地配置
local.properties

# 日志
*.log
logcat.txt
error.txt

# 模型文件（大文件）
*.onnx

# OS
.DS_Store
Thumbs.db
```

### 项目上传示例

```bash
# 进入项目目录
cd d:\data\Desktop\facefound\android_project

# 检查状态
git status

# 添加所有修改
git add -A

# 提交（使用规范格式）
git commit -m "feat: 添加视频人脸识别功能

- 新增 VideoProcessor 处理视频帧检测和编码
- 新增 VideoScreen UI 界面
- 修复编码器 EOS 错误发送导致视频不完整
- 优化性能：减少 bitmap 拷贝和 UI 更新频率
- 优化 GUI：侧滑菜单动效、卡片圆角、状态图标"

# 推送到 GitHub
git push origin main
```

---

> 最后更新：2026-05-05
> 适用项目：FaceFound Android
