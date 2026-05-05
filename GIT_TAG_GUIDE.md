# Git 打 Tag 操作指南

## 创建并推送 Tag

```bash
# 查看当前版本
git tag -l

# 创建 annotated tag
git tag -a v2.1.0 -m 'FaceFound v2.1.0 - 功能简述'

# 推送到远程
git push origin v2.1.0

# 或推送所有 tag
git push origin --tags
```

## 删除 Tag

```bash
# 删除本地 tag
git tag -d v2.1.0

# 删除远程 tag
git push origin --delete v2.1.0
```

## 查看 Tag

```bash
# 列出所有 tag
git tag

# 查看 tag 详情
git show v2.1.0

# 查看 tag 信息
git tag -n
```

## 常用 Tag 命令速查

| 命令 | 作用 |
|------|------|
| `git tag` | 列出所有 tag |
| `git tag -a v1.0 -m "msg"` | 创建带注释的 tag |
| `git push origin v1.0` | 推送指定 tag |
| `git push origin --tags` | 推送所有 tag |
| `git tag -d v1.0` | 删除本地 tag |
| `git push origin --delete v1.0` | 删除远程 tag |
| `git show v1.0` | 查看 tag 详情 |

## 注意事项

- Tag 创建后必须推送才会出现在 GitHub Releases 页面
- 推荐使用 annotated tag（`-a` 参数）便于追溯
- Tag 名遵循语义化版本：`v主版本.次版本.修订`
- `MD_UPDATE_REPORT.md` 已加入 `.gitignore`，不会提交到仓库
