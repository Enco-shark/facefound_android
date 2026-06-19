package com.Enco.facerecognition.util // 声明包名为工具模块

import android.content.Context // 导入Android上下文类
import android.util.Log // 导入Android日志工具
import org.json.JSONArray // 导入JSON数组类（未使用但保留）
import org.json.JSONObject // 导入JSON对象类，用于索引文件读写
import java.io.DataInputStream // 导入数据输入流，用于读取二进制数据
import java.io.DataOutputStream // 导入数据输出流，用于写入二进制数据
import java.io.File // 导入文件类
import java.nio.ByteBuffer // 导入字节缓冲区
import java.nio.ByteOrder // 导入字节序枚举

class TemplateRepository(context: Context) { // 模板仓库类，负责模板的持久化存储和加载

    companion object { // 伴生对象，存放静态常量
        private const val TAG = "TemplateRepository" // 日志标签
        private const val TEMPLATES_DIR = "face_templates" // 模板存储目录名
        private const val INDEX_FILE = "index.json" // 索引文件名
    }

    private val templatesDir = File(context.filesDir, TEMPLATES_DIR).also { // 创建模板目录引用
        if (!it.exists()) it.mkdirs() // 目录不存在时创建
    }
    private val indexFile = File(templatesDir, INDEX_FILE) // 创建索引文件引用

    fun loadAll(): Map<String, FloatArray> { // 加载所有已保存的模板
        if (!indexFile.exists()) { // 索引文件不存在
            Log.i(TAG, "模板索引文件不存在，返回空") // 打印提示日志
            return emptyMap() // 返回空Map
        }

        return try { // 尝试解析索引文件
            val json = JSONObject(indexFile.readText()) // 读取并解析JSON索引
            val result = mutableMapOf<String, FloatArray>() // 创建可变结果Map

            json.keys().forEach { name -> // 遍历索引中每个模板名
                val dataFile = File(templatesDir, json.getString(name)) // 根据索引获取数据文件路径
                if (dataFile.exists()) { // 数据文件存在
                    val embedding = loadEmbedding(dataFile) // 加载嵌入向量
                    if (embedding != null) { // 加载成功
                        result[name] = embedding // 添加到结果Map
                    }
                }
            }

            Log.i(TAG, "已加载 ${result.size} 个模板") // 打印加载数量
            result // 返回结果Map
        } catch (e: Exception) { // 解析异常
            Log.e(TAG, "加载模板失败: ${e.message}", e) // 打印错误日志
            emptyMap() // 返回空Map
        }
    }

    fun save(templates: Map<String, FloatArray>) { // 保存所有模板到磁盘
        try { // 异常保护
            val tmpDir = File(templatesDir.parent, "${TEMPLATES_DIR}_tmp") // 临时目录，用于原子写入
            val backupDir = File(templatesDir.parent, "${TEMPLATES_DIR}_backup") // 备份目录

            if (tmpDir.exists()) tmpDir.deleteRecursively() // 清理旧临时目录
            tmpDir.mkdirs() // 创建临时目录

            val index = JSONObject() // 创建JSON索引对象

            templates.forEach { (name, embedding) -> // 遍历每个模板
                val safeName = name.replace(Regex("[^a-zA-Z0-9_\\-\\u4e00-\\u9fff]"), "_") // 清理文件名中的特殊字符
                val dataFile = File(tmpDir, "${safeName}_${embedding.contentHashCode()}.emb") // 创建数据文件路径，包含哈希避免冲突
                saveEmbedding(dataFile, embedding) // 保存嵌入向量到文件
                index.put(name, dataFile.name) // 在索引中记录模板名到文件名的映射
            }

            val tmpIndexFile = File(tmpDir, INDEX_FILE) // 临时索引文件路径
            tmpIndexFile.writeText(index.toString(2)) // 写入格式化的JSON索引

            if (templatesDir.exists()) { // 原目录存在
                if (backupDir.exists()) backupDir.deleteRecursively() // 清理旧备份
                if (!templatesDir.renameTo(backupDir)) { // 尝试将原目录重命名为备份
                    Log.w(TAG, "备份原目录失败，尝试直接删除") // 重命名失败
                    templatesDir.deleteRecursively() // 直接删除原目录
                }
            }

            val renamed = tmpDir.renameTo(templatesDir) // 将临时目录重命名为正式目录
            if (!renamed) { // 重命名失败
                Log.e(TAG, "重命名临时目录失败，尝试恢复备份") // 打印错误
                if (backupDir.exists()) { // 备份存在
                    backupDir.renameTo(templatesDir) // 恢复备份
                }
                Log.e(TAG, "保存模板失败：无法重命名临时目录") // 打印最终错误
                return // 提前返回
            }

            if (backupDir.exists()) { // 保存成功，清理备份
                backupDir.deleteRecursively() // 删除备份目录
            }

            Log.i(TAG, "已保存 ${templates.size} 个模板") // 打印保存成功日志
        } catch (e: Exception) { // 保存异常
            Log.e(TAG, "保存模板失败: ${e.message}", e) // 打印错误日志
        }
    }

    private fun saveEmbedding(file: File, embedding: FloatArray) { // 保存单个嵌入向量到文件
        DataOutputStream(file.outputStream()).use { out -> // 创建数据输出流，自动关闭
            out.writeInt(embedding.size) // 先写入向量维度（4字节整数）
            val buffer = ByteBuffer.allocate(embedding.size * 4).order(ByteOrder.LITTLE_ENDIAN) // 分配小端字节序缓冲区
            embedding.forEach { buffer.putFloat(it) } // 将每个浮点数写入缓冲区
            out.write(buffer.array()) // 将缓冲区字节数组写入流
        }
    }

    private fun loadEmbedding(file: File): FloatArray? { // 从文件加载嵌入向量
        return try { // 异常保护
            DataInputStream(file.inputStream()).use { input -> // 创建数据输入流，自动关闭
                val size = input.readInt() // 读取向量维度
                val data = ByteArray(size * 4) // 分配字节数组（每个float 4字节）
                input.readFully(data) // 读取全部字节
                val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN) // 包装为小端字节序缓冲区
                FloatArray(size) { buffer.float } // 逐个读取浮点数
            }
        } catch (e: Exception) { // 加载异常
            Log.e(TAG, "加载嵌入向量失败 ${file.name}: ${e.message}") // 打印错误日志
            null // 返回null
        }
    }

    fun clear() { // 清除所有模板
        templatesDir.listFiles()?.forEach { it.delete() } // 删除目录下所有文件
        Log.i(TAG, "所有模板已清除") // 打印清除日志
    }

    fun getTemplateCount(): Int { // 获取模板数量
        if (!indexFile.exists()) return 0 // 索引文件不存在返回0
        return try { // 尝试解析
            JSONObject(indexFile.readText()).length() // 解析JSON并返回键值对数量
        } catch (_: Exception) { 0 } // 解析失败返回0
    }
}
