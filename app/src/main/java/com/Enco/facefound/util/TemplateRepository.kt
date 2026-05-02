package com.Enco.facefound.util

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TemplateRepository(context: Context) {

    companion object {
        private const val TAG = "TemplateRepository"
        private const val TEMPLATES_DIR = "face_templates"
        private const val INDEX_FILE = "index.json"
    }

    private val templatesDir = File(context.filesDir, TEMPLATES_DIR).also {
        if (!it.exists()) it.mkdirs()
    }
    private val indexFile = File(templatesDir, INDEX_FILE)

    fun loadAll(): Map<String, FloatArray> {
        if (!indexFile.exists()) {
            Log.i(TAG, "模板索引文件不存在，返回空")
            return emptyMap()
        }

        return try {
            val json = JSONObject(indexFile.readText())
            val result = mutableMapOf<String, FloatArray>()

            json.keys().forEach { name ->
                val dataFile = File(templatesDir, json.getString(name))
                if (dataFile.exists()) {
                    val embedding = loadEmbedding(dataFile)
                    if (embedding != null) {
                        result[name] = embedding
                    }
                }
            }

            Log.i(TAG, "已加载 ${result.size} 个模板")
            result
        } catch (e: Exception) {
            Log.e(TAG, "加载模板失败: ${e.message}", e)
            emptyMap()
        }
    }

    fun save(templates: Map<String, FloatArray>) {
        try {
            val tmpDir = File(templatesDir.parent, "${TEMPLATES_DIR}_tmp")
            val backupDir = File(templatesDir.parent, "${TEMPLATES_DIR}_backup")
            
            if (tmpDir.exists()) tmpDir.deleteRecursively()
            tmpDir.mkdirs()

            val index = JSONObject()

            templates.forEach { (name, embedding) ->
                val safeName = name.replace(Regex("[^a-zA-Z0-9_\\-\\u4e00-\\u9fff]"), "_")
                val dataFile = File(tmpDir, "${safeName}_${embedding.contentHashCode()}.emb")
                saveEmbedding(dataFile, embedding)
                index.put(name, dataFile.name)
            }

            val tmpIndexFile = File(tmpDir, INDEX_FILE)
            tmpIndexFile.writeText(index.toString(2))

            if (templatesDir.exists()) {
                if (backupDir.exists()) backupDir.deleteRecursively()
                if (!templatesDir.renameTo(backupDir)) {
                    Log.w(TAG, "备份原目录失败，尝试直接删除")
                    templatesDir.deleteRecursively()
                }
            }
            
            val renamed = tmpDir.renameTo(templatesDir)
            if (!renamed) {
                Log.e(TAG, "重命名临时目录失败，尝试恢复备份")
                if (backupDir.exists()) {
                    backupDir.renameTo(templatesDir)
                }
                Log.e(TAG, "保存模板失败：无法重命名临时目录")
                return
            }
            
            if (backupDir.exists()) {
                backupDir.deleteRecursively()
            }

            Log.i(TAG, "已保存 ${templates.size} 个模板")
        } catch (e: Exception) {
            Log.e(TAG, "保存模板失败: ${e.message}", e)
        }
    }

    private fun saveEmbedding(file: File, embedding: FloatArray) {
        DataOutputStream(file.outputStream()).use { out ->
            out.writeInt(embedding.size)
            val buffer = ByteBuffer.allocate(embedding.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            embedding.forEach { buffer.putFloat(it) }
            out.write(buffer.array())
        }
    }

    private fun loadEmbedding(file: File): FloatArray? {
        return try {
            DataInputStream(file.inputStream()).use { input ->
                val size = input.readInt()
                val data = ByteArray(size * 4)
                input.readFully(data)
                val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
                FloatArray(size) { buffer.float }
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载嵌入向量失败 ${file.name}: ${e.message}")
            null
        }
    }

    fun clear() {
        templatesDir.listFiles()?.forEach { it.delete() }
        Log.i(TAG, "所有模板已清除")
    }

    fun getTemplateCount(): Int {
        if (!indexFile.exists()) return 0
        return try {
            JSONObject(indexFile.readText()).length()
        } catch (_: Exception) { 0 }
    }
}
