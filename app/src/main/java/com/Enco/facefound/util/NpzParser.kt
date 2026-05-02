package com.Enco.facefound.util

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipInputStream

object NpzParser {

    private const val TAG = "NpzParser"

    suspend fun parseFromUri(context: Context, uri: Uri): Map<String, FloatArray> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始解析文件: $uri")
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("无法打开文件: $uri")
                inputStream.use { stream -> parseNpz(stream) }
            } catch (e: Exception) {
                Log.e(TAG, "解析 NPZ 失败: ${e.javaClass.simpleName} - ${e.message}", e)
                emptyMap()
            }
        }

    private fun parseNpz(inputStream: InputStream): Map<String, FloatArray> {
        val result = mutableMapOf<String, FloatArray>()
        val zipStream = ZipInputStream(inputStream)

        var namesArray: Array<String>? = null
        var embeddingsArray: Array<FloatArray>? = null
        val rawEntries = mutableMapOf<String, ByteArray>()

        try {
            var entry = zipStream.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".npy")) {
                    try {
                        val data = zipStream.readBytes()
                        val fileName = entry.name.split("/").last().removeSuffix(".npy")
                        val hexPreview = data.take(32).joinToString(" ") { "%02X".format(it) }
                        Log.d(TAG, "ZIP 条目: $fileName (${data.size} bytes) hex: $hexPreview")
                        rawEntries[fileName] = data
                    } catch (e: Exception) {
                        Log.e(TAG, "读取条目 ${entry.name} 失败", e)
                    }
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
        } finally {
            try {
                zipStream.close()
            } catch (e: Exception) {
                Log.w(TAG, "关闭 ZipInputStream 失败: ${e.message}")
            }
        }

        if (rawEntries.isEmpty()) {
            Log.e(TAG, "ZIP 中没有找到 .npy 文件")
            Log.e(TAG, "提示: 确认文件是有效的 NPZ 格式（ZIP 包含 .npy 文件）")
            return emptyMap()
        }

        Log.i(TAG, "rawEntries 包含 ${rawEntries.size} 个条目: ${rawEntries.keys.joinToString(", ")}")

        // 查找备选名字 key (names / labels / name_list / template_names 等)
        val nameKeyCandidates = listOf("names", "labels", "name_list", "template_names", "ids", "person_ids", "label")
        val embeddingKeyCandidates = listOf("embeddings", "features", "templates", "template_data", "embs", "vectors")

        val actualNameKey = nameKeyCandidates.firstOrNull { rawEntries.containsKey(it) }
        val actualEmbKey = embeddingKeyCandidates.firstOrNull { rawEntries.containsKey(it) }

        // 如果用的是备选 key 但与 "names"/"embeddings" 不同，做映射
        if ((actualNameKey != null && actualNameKey != "names") || (actualEmbKey != null && actualEmbKey != "embeddings")) {
            if (actualNameKey != null && actualNameKey != "names") {
                rawEntries["names"] = rawEntries.remove(actualNameKey)!!
                Log.i(TAG, "将 '$actualNameKey' 映射为 'names'")
            }
            if (actualEmbKey != null && actualEmbKey != "embeddings") {
                rawEntries["embeddings"] = rawEntries.remove(actualEmbKey)!!
                Log.i(TAG, "将 '$actualEmbKey' 映射为 'embeddings'")
            }
        }

        // 尝试解析 embeddings（无论是否有 names）
        val hasEmbeddingsOnly = rawEntries.containsKey("embeddings") && !rawEntries.containsKey("names")
        val hasNamesOnly = rawEntries.containsKey("names") && !rawEntries.containsKey("embeddings")

        if (hasEmbeddingsOnly) {
            Log.i(TAG, "仅有 embeddings，无 names，使用索引名称")
            embeddingsArray = parseEmbeddingsNpy(rawEntries["embeddings"]!!)
            if (embeddingsArray != null && embeddingsArray.isNotEmpty()) {
                for (i in embeddingsArray.indices) {
                    result["Person_$i"] = embeddingsArray[i]
                }
                Log.i(TAG, "✅ 索引名称回退: ${result.size} 个模板 (Person_0 ~ Person_${result.size - 1})")
                return result
            }
            Log.w(TAG, "embeddings 解析失败，继续尝试其他策略")
        }

        if (hasNamesOnly) {
            Log.i(TAG, "仅有 names，无 embeddings 数据，无法创建模板")
            return emptyMap()
        }

        // 桌面版格式: names + embeddings
        if (rawEntries.containsKey("names") && rawEntries.containsKey("embeddings")) {
            Log.i(TAG, "检测到 names+embeddings 桌面版格式，开始解析...")
            namesArray = parseNamesNpy(rawEntries["names"]!!)
            embeddingsArray = parseEmbeddingsNpy(rawEntries["embeddings"]!!)

            if (namesArray == null) {
                Log.w(TAG, "names 解析失败，将使用索引名称")
            }
            if (embeddingsArray == null) {
                Log.w(TAG, "embeddings 解析失败，将尝试单文件格式")
            }

            if (namesArray != null && embeddingsArray != null && namesArray.size == embeddingsArray.size) {
                val duplicateNames = namesArray.groupBy { it }.filter { it.value.size > 1 }.keys
                if (duplicateNames.isNotEmpty()) {
                    Log.w(TAG, "发现 ${duplicateNames.size} 个重复名字，将添加索引后缀: ${duplicateNames.take(5).joinToString()}")
                }
                namesArray.zip(embeddingsArray).forEachIndexed { idx, (name, embedding) ->
                    val key = if (duplicateNames.contains(name)) "${name}_$idx" else name
                    result[key] = embedding
                }
                val sampleNames = result.keys.take(5).joinToString(", ")
                val sampleDim = result.values.firstOrNull()?.size ?: 0
                Log.i(TAG, "桌面版格式解析完成: ${result.size} 个模板 (dim=$sampleDim, 样本: $sampleNames)")
                return result
            }

            if (embeddingsArray != null && embeddingsArray.isNotEmpty()) {
                val useNames = namesArray ?: Array(embeddingsArray.size) { "Person_$it" }
                val count = minOf(useNames.size, embeddingsArray.size)
                for (i in 0 until count) {
                    result[useNames[i]] = embeddingsArray[i]
                }
                if (namesArray == null) {
                    Log.i(TAG, "⚠️ 使用索引名称回退: ${result.size} 个模板 (Person_0 ~ Person_${count - 1})")
                } else {
                    Log.i(TAG, "⚠️ 数量不匹配回退: ${result.size} 个模板")
                }
                return result
            }

            Log.w(TAG, "names(${namesArray?.size ?: 0}) 和 embeddings(${embeddingsArray?.size ?: 0}) 不匹配，尝试单文件")
        } else {
            Log.i(TAG, "未检测到标准 names+embeddings，尝试单文件格式解析")
        }

        // 单文件格式: 每个 .npy 文件是一个人脸模板
        var singleFileCount = 0
        rawEntries.forEach { (fileName, data) ->
            if (fileName == "names" || fileName == "embeddings") return@forEach
            try {
                val embedding = parseNpy(ByteArrayInputStream(data))
                if (embedding != null && embedding.isNotEmpty()) {
                    result[fileName] = embedding
                    singleFileCount++
                    Log.d(TAG, "单文件模板: $fileName (${embedding.size} 维)")
                } else {
                    Log.w(TAG, "单文件 $fileName 解析返回空")
                }
            } catch (e: Exception) {
                Log.e(TAG, "解析 $fileName 失败: ${e.message}")
            }
        }
        Log.i(TAG, "单文件格式解析: $singleFileCount 个模板")

        // 如果单文件也没解析出来，回退：对 names+embeddings 组合
        if (result.isEmpty() && namesArray != null && embeddingsArray != null) {
            val count = minOf(namesArray.size, embeddingsArray.size)
            for (i in 0 until count) {
                result[namesArray[i]] = embeddingsArray[i]
            }
            Log.i(TAG, "回退解析完成: ${result.size} 个模板")
        }

        // == 最后防线: raw float32 暴力回退 ==
        if (result.isEmpty()) {
            Log.w(TAG, "所有标准策略均失败，进入 raw float32 暴力回退...")
            for ((fileName, data) in rawEntries) {
                try {
                    val (_, rawFloats) = tryRawFloat32Fallback(ByteArrayInputStream(data))
                    if (rawFloats != null && rawFloats.isNotEmpty()) {
                        // 扔给暴力维度猜测
                        val embeds = parseEmbeddingsNpy(data) ?: arrayOf(rawFloats)
                        embeds.forEachIndexed { idx, emb ->
                            val name = if (embeds.size == 1) fileName else "${fileName}_$idx"
                            result[name] = emb
                        }
                        Log.i(TAG, "raw 回退从 '$fileName' 解析出 ${embeds.size} 个模板")
                    }
                } catch (_: Exception) {}
            }
        }

        if (result.isEmpty()) {
            Log.e(TAG, "❌ NPZ 解析彻底失败: 含 raw 回退在内的所有策略均未解析出模板")
            Log.e(TAG, "调试信息 - rawEntries keys: ${rawEntries.keys.joinToString(", ")}")
            if (namesArray != null) Log.e(TAG, "names 解析结果: ${namesArray.size} 个名字")
            if (embeddingsArray != null) Log.e(TAG, "embeddings 解析结果: ${embeddingsArray.size} 个嵌入向量")
        } else {
            Log.i(TAG, "✅ NPZ 解析成功，共 ${result.size} 个模板")
            for (name in result.keys) {
                Log.d(TAG, "  模板: $name (${result[name]?.size ?: 0} 维)")
            }
        }
        return result
    }

    private fun parseNamesNpy(data: ByteArray): Array<String>? {
        return try {
            val inputStream = DataInputStream(ByteArrayInputStream(data))

            val magic = ByteArray(6)
            inputStream.readFully(magic)
            if (magic[0] != 0x93.toByte() || magic[1] != 'N'.code.toByte() ||
                magic[2] != 'U'.code.toByte() || magic[3] != 'M'.code.toByte() ||
                magic[4] != 'P'.code.toByte() || magic[5] != 'Y'.code.toByte()
            ) {
                Log.w(TAG, "names.npy 魔数异常")
            }

            val majorVersion = inputStream.readUnsignedByte()
            inputStream.readUnsignedByte()
            val headerLen = readHeaderLen(inputStream, majorVersion)

            val headerBytes = ByteArray(headerLen)
            inputStream.readFully(headerBytes)
            val header = String(headerBytes, Charsets.UTF_8)
            Log.d(TAG, "names header: ${header.take(100)}")

            val dtype = extractDtype(header)
            val shape = extractShape(header)
            val count = if (shape.isNotEmpty()) shape[0] else 0
            Log.d(TAG, "names header shape=$shape, dtype=$dtype, 预期数量: $count")

            val remaining = inputStream.readBytes()
            inputStream.close()

            val unicodeResult = tryParseUnicodeStrings(remaining, dtype, count)
            if (unicodeResult != null && unicodeResult.size == count && count > 0) {
                Log.d(TAG, "Unicode 策略解析到 ${unicodeResult.size} 个名字")
                return unicodeResult.toTypedArray()
            }

            val result = parseNamesData(remaining, count)
            Log.d(TAG, "解析到 ${result.size} 个名字")
            result.toTypedArray()

        } catch (e: Exception) {
            Log.e(TAG, "解析 names.npy 失败: ${e.message}", e)
            null
        }
    }

    private fun tryParseUnicodeStrings(data: ByteArray, dtype: String, expectedCount: Int): List<String>? {
        val charSizeMatch = Regex("<U(\\d+)").find(dtype)
        if (charSizeMatch == null) return null
        val charSize = charSizeMatch.groupValues[1].toIntOrNull() ?: return null
        if (charSize <= 0 || charSize > 256) return null

        if (expectedCount <= 0) return null

        val bytesPerElement = charSize * 4
        val neededBytes = expectedCount * bytesPerElement
        if (data.size < neededBytes) {
            Log.w(TAG, "Unicode 解析: 数据不足 (需要 $neededBytes, 实际 ${data.size})")
            return null
        }

        val result = mutableListOf<String>()
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until expectedCount) {
            val offset = i * bytesPerElement
            val sb = StringBuilder()
            for (c in 0 until charSize) {
                val codePoint = buf.getInt(offset + c * 4)
                if (codePoint == 0) break
                sb.appendCodePoint(codePoint)
            }
            result.add(sb.toString())
        }

        val nonEmpty = result.count { it.isNotBlank() }
        Log.d(TAG, "Unicode<$charSize> 解析: ${result.size} 个, 非空 $nonEmpty 个")
        if (nonEmpty < expectedCount * 0.5) return null

        return result
    }

    /**
     * raw float32 回退: 将剩余字节解释为 float32 little-endian
     */
    private fun tryRawFloat32Fallback(inputStream: InputStream): Pair<List<Int>, FloatArray?> {
        return try {
            val remaining = inputStream.readBytes()
            if (remaining.size < 4) {
                Log.w(TAG, "raw 回退: 数据不足 (${remaining.size} bytes)")
                return Pair(emptyList(), null)
            }
            // 对齐到 4 字节边界
            val alignedSize = remaining.size - (remaining.size % 4)
            if (alignedSize < 4) return Pair(emptyList(), null)

            val count = alignedSize / 4
            val buf = ByteBuffer.wrap(remaining, 0, alignedSize).order(ByteOrder.LITTLE_ENDIAN)
            val result = FloatArray(count)
            for (i in 0 until count) result[i] = buf.float

            // 检查合理性：不应全是 NaN/Inf
            val validCount = result.count { it.isFinite() }
            if (validCount < count * 0.5f) {
                Log.w(TAG, "raw 回退: 仅 $validCount/$count 个有效浮点数，数据可能不是 float32")
                return Pair(listOf(count), null)
            }

            Log.i(TAG, "raw float32 回退成功: $count 个浮点数")
            Pair(listOf(count), result)
        } catch (e: Exception) {
            Log.e(TAG, "raw 回退失败: ${e.message}")
            Pair(emptyList(), null)
        }
    }

    private fun parseNamesData(data: ByteArray, expectedCount: Int): List<String> {
        // 策略1: Python numpy object array pickle 格式
        val pickle = tryParsePickleStrings(data, expectedCount)
        if (pickle.size >= expectedCount && expectedCount > 0) return pickle
        if (pickle.isNotEmpty() && expectedCount == 0) return pickle

        // 策略2: 指针数组 + 字符串
        val pointer = tryParsePointerStrings(data, expectedCount)
        if (pointer.size >= expectedCount && expectedCount > 0) return pointer

        // 策略3: 扫描所有可读 UTF-8 字符串
        val scanned = scanReadableStrings(data, expectedCount)
        if (scanned.isNotEmpty()) return scanned

        // 策略4: null-terminated 字符串
        val nullTerm = tryParseNullTermStrings(data, expectedCount)
        if (nullTerm.isNotEmpty()) return nullTerm

        return pickle.ifEmpty { pointer.ifEmpty { scanned } }
    }

    private fun tryParsePickleStrings(data: ByteArray, expectedCount: Int): List<String> {
        val result = mutableListOf<String>()
        try {
            var i = 0
            while (i < data.size && (expectedCount == 0 || result.size < expectedCount)) {
                when (data[i].toInt() and 0xFF) {
                    0x55 -> { // SHORT_BINUNICODE (pickle protocol 2+)
                        if (i + 2 <= data.size) {
                            val len = ((data[i + 1].toInt() and 0xFF) or
                                      ((data[i + 2].toInt() and 0xFF) shl 8))
                            if (len in 1..1024 && i + 3 + len <= data.size) {
                                val str = String(data, i + 3, len, Charsets.UTF_8)
                                if (str.isNotBlank() && str.all { c -> !c.isISOControl() || c == ' ' }) {
                                    result.add(str)
                                }
                            }
                            i += 3 + len
                            continue
                        }
                    }
                    0x8C -> { // SHORT_BINUNICODE (pickle protocol 4+)
                        if (i + 1 < data.size) {
                            val len = data[i + 1].toInt() and 0xFF
                            if (len in 1..255 && i + 2 + len <= data.size) {
                                val str = String(data, i + 2, len, Charsets.UTF_8)
                                if (str.isNotBlank() && str.all { c -> !c.isISOControl() || c == ' ' }) {
                                    result.add(str)
                                }
                            }
                            i += 2 + len
                            continue
                        }
                    }
                    0x8D -> { // BINUNICODE (pickle protocol 4+)
                        if (i + 5 <= data.size) {
                            val len = ByteBuffer.wrap(data, i + 1, 4)
                                .order(ByteOrder.LITTLE_ENDIAN).int
                            if (len in 1..1024 && i + 5 + len <= data.size) {
                                val str = String(data, i + 5, len, Charsets.UTF_8)
                                if (str.isNotBlank() && str.all { c -> !c.isISOControl() || c == ' ' }) {
                                    result.add(str)
                                }
                            }
                            i += 5 + len
                            continue
                        }
                    }
                }
                i++
            }
            Log.d(TAG, "pickle 策略解析到 ${result.size} 个字符串")
        } catch (_: Exception) {}
        return result
    }

    private fun tryParsePointerStrings(data: ByteArray, expectedCount: Int): List<String> {
        val result = mutableListOf<String>()
        try {
            if (data.size < expectedCount * 8 || expectedCount <= 0) return result

            val pointers = mutableListOf<Int>()
            for (i in 0 until expectedCount) {
                val offset = i * 8
                val ptr = ByteBuffer.wrap(data, offset, 8)
                    .order(ByteOrder.LITTLE_ENDIAN).long.toInt()
                if (ptr in 0 until data.size) {
                    pointers.add(ptr)
                }
            }

            if (pointers.size != expectedCount) return result

            for (ptr in pointers) {
                val strLen = ByteBuffer.wrap(data, ptr, 8)
                    .order(ByteOrder.LITTLE_ENDIAN).long.toInt()
                if (strLen in 1..1024 && ptr + 8 + strLen <= data.size) {
                    val str = String(data, ptr + 8, strLen, Charsets.UTF_8)
                    if (str.isNotBlank()) result.add(str)
                }
            }
            Log.d(TAG, "pointer 策略解析到 ${result.size} 个字符串")
        } catch (_: Exception) {}
        return result
    }

    private fun scanReadableStrings(data: ByteArray, expectedCount: Int): List<String> {
        val result = mutableListOf<String>()
        try {
            var start = -1
            for (i in data.indices) {
                val b = data[i].toInt() and 0xFF
                if (b in 32..126 || b >= 192) { // ASCII printable or UTF-8 continuation
                    if (start < 0) start = i
                } else {
                    if (start >= 0) {
                        val len = i - start
                        if (len in 2..128) {
                            try {
                                val str = String(data, start, len, Charsets.UTF_8)
                                if (str.isNotBlank() && str.none { it.isISOControl() && it != ' ' }) {
                                    result.add(str)
                                    if (expectedCount > 0 && result.size >= expectedCount) return result
                                }
                            } catch (_: Exception) {}
                        }
                        start = -1
                    }
                }
            }
            Log.d(TAG, "scan 策略解析到 ${result.size} 个字符串")
        } catch (_: Exception) {}
        return result
    }

    private fun tryParseNullTermStrings(data: ByteArray, expectedCount: Int): List<String> {
        val result = mutableListOf<String>()
        try {
            var offset = 0
            while (offset < data.size && (expectedCount == 0 || result.size < expectedCount)) {
                var zeroPos = -1
                for (i in offset until data.size) {
                    if (data[i] == 0.toByte()) {
                        zeroPos = i
                        break
                    }
                }
                if (zeroPos > offset) {
                    val str = String(data, offset, zeroPos - offset, Charsets.UTF_8)
                        .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")
                    if (str.isNotBlank() && str.length > 1) {
                        result.add(str)
                    }
                    offset = zeroPos + 1
                } else {
                    break
                }
            }
            Log.d(TAG, "null-term 策略解析到 ${result.size} 个字符串")
        } catch (_: Exception) {}
        return result
    }

    private fun parseEmbeddingsNpy(data: ByteArray): Array<FloatArray>? {
        return try {
            val (shape, flatArray) = parseNpyWithShape(ByteArrayInputStream(data))
            if (flatArray == null || flatArray.isEmpty()) return null

            if (shape.size == 2 && shape[1] > 0 && shape[0] > 0) {
                val dim = shape[1]
                val count = shape[0]
                Log.d(TAG, "embeddings: $count x $dim (从 header 读取)")
                val result = Array(count) { i -> flatArray.copyOfRange(i * dim, (i + 1) * dim) }
                for (i in result.indices) {
                    l2Normalize(result[i])
                }
                Log.d(TAG, "embeddings 已 L2 归一化, 样本[0]前5: ${result[0].take(5).joinToString(",") { "%.4f".format(it) }}")
                return result
            }

            val commonDims = listOf(512, 256, 128, 1024, 768, 2560, 320, 2048)
            for (dim in commonDims) {
                if (flatArray.size % dim == 0 && flatArray.size >= dim) {
                    val count = flatArray.size / dim
                    Log.i(TAG, "embeddings 暴力猜测: $count x $dim")
                    val result = Array(count) { i -> flatArray.copyOfRange(i * dim, (i + 1) * dim) }
                    for (i in result.indices) {
                        l2Normalize(result[i])
                    }
                    return result
                }
            }

            Log.i(TAG, "embeddings 作为单个 ${flatArray.size} 维嵌入")
            arrayOf(flatArray)
        } catch (e: Exception) {
            Log.e(TAG, "解析 embeddings 失败: ${e.message}", e)
            null
        }
    }

    /**
     * 一次读取完成 header 解析和 float 数据提取，避免重复 IO
     * @return Pair(shape, floatArray)
     */
    private fun parseNpyWithShape(inputStream: InputStream): Pair<List<Int>, FloatArray?> {
        return try {
            val dataInput = DataInputStream(inputStream)
            val magic = ByteArray(6)
            dataInput.readFully(magic)

            if (magic[0] != 0x93.toByte() || magic[1] != 'N'.code.toByte() ||
                magic[2] != 'U'.code.toByte() || magic[3] != 'M'.code.toByte() ||
                magic[4] != 'P'.code.toByte() || magic[5] != 'Y'.code.toByte()
            ) {
                Log.w(TAG, "NPY 魔数异常: ${magic.joinToString("") { "%02X".format(it) }}, 尝试 raw float32 回退")
                dataInput.close()
                return tryRawFloat32Fallback(inputStream)
            }

            val majorVersion = dataInput.readUnsignedByte()
            dataInput.readUnsignedByte()
            val headerLen = readHeaderLen(dataInput, majorVersion)
            val headerBytes = ByteArray(headerLen)
            dataInput.readFully(headerBytes)
            val header = String(headerBytes, Charsets.UTF_8)
            Log.d(TAG, "NPY header: ${header.take(150)}")

            val dtype = extractDtype(header)
            val shape = extractShape(header)

            if (shape.isEmpty()) {
                Log.w(TAG, "NPY shape 为空")
                dataInput.close()
                return Pair(emptyList(), null)
            }

            val totalElements = shape.fold(1L) { acc, i -> acc * i }
            if (totalElements > 10_000_000L) {
                Log.w(TAG, "NPY 数组过大: $totalElements 个元素 (限制10M)，跳过")
                dataInput.close()
                return Pair(shape, null)
            }
            if (totalElements <= 0) {
                Log.w(TAG, "NPY 元素数为 0")
                dataInput.close()
                return Pair(shape, null)
            }

            val count = totalElements.toInt()
            val result = when {
                dtype.contains("f4") || dtype.contains("float32") -> readFloat32Batch(dataInput, count)
                dtype.contains("f8") || dtype.contains("float64") -> readFloat64Batch(dataInput, count)
                dtype.contains("i4") || dtype.contains("int32") -> readToFloatBatch(dataInput, count, 4) { buf -> buf.int.toFloat() }
                dtype.contains("i8") || dtype.contains("int64") -> readToFloatBatch(dataInput, count, 8) { buf -> buf.long.toFloat() }
                else -> {
                    Log.w(TAG, "未知 dtype '$dtype'，尝试 float32 回退")
                    readFloat32Batch(dataInput, count)
                }
            }

            dataInput.close()
            Log.d(TAG, "NPY 解析完成: ${result?.size ?: 0} 个 float, shape=$shape, dtype=$dtype")
            Pair(shape, result)

        } catch (e: Exception) {
            Log.e(TAG, "NPY 解析异常: ${e.javaClass.simpleName} - ${e.message}")
            Pair(emptyList(), null)
        }
    }

    private fun parseNpy(inputStream: InputStream): FloatArray? {
        val (_, result) = parseNpyWithShape(inputStream)
        return result
    }

    private fun readFloat32Batch(input: DataInputStream, count: Int): FloatArray {
        val result = FloatArray(count)
        val buf = ByteArray(count * 4)
        input.readFully(buf)
        val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until count) result[i] = bb.float
        return result
    }

    private fun readFloat64Batch(input: DataInputStream, count: Int): FloatArray {
        val result = FloatArray(count)
        val buf = ByteArray(count * 8)
        input.readFully(buf)
        val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until count) result[i] = bb.double.toFloat()
        return result
    }

    private fun readToFloatBatch(input: DataInputStream, count: Int, elemSize: Int, convert: (ByteBuffer) -> Float): FloatArray {
        val result = FloatArray(count)
        val buf = ByteArray(count * elemSize)
        input.readFully(buf)
        val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
        var offset = 0
        for (i in 0 until count) {
            bb.position(offset)
            result[i] = convert(bb)
            offset += elemSize
        }
        return result
    }

    private fun readHeaderLen(input: DataInputStream, majorVersion: Int): Int {
        return when (majorVersion) {
            1 -> {
                val b = ByteArray(2); input.readFully(b)
                ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
            }
            2, 3 -> {
                val b = ByteArray(4); input.readFully(b)
                ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).int
            }
            else -> throw Exception("不支持的 NPY 版本: $majorVersion")
        }
    }

    private fun l2Normalize(vector: FloatArray) {
        var sum = 0f
        for (v in vector) sum += v * v
        val norm = kotlin.math.sqrt(sum)
        if (norm > 0f) {
            for (i in vector.indices) vector[i] /= norm
        }
    }

    private fun extractDtype(header: String): String {
        val patterns = listOf(
            "'descr':\\s*'([^']+)'".toRegex(),
            "\"descr\":\\s*\"([^\"]+)\"".toRegex()
        )
        for (p in patterns) {
            val m = p.find(header)
            if (m != null) return m.groupValues[1]
        }
        return "<f4"
    }

    private fun extractShape(header: String): List<Int> {
        val patterns = listOf(
            "'shape':\\s*\\(([^)]*)\\)".toRegex(),
            "\"shape\":\\s*\\(([^)]*)\\)".toRegex(),
            "'shape':\\s*\\[([^]]*)\\]".toRegex()
        )
        for (p in patterns) {
            val m = p.find(header)
            if (m != null) {
                val s = m.groupValues[1].trim()
                if (s.isEmpty()) return emptyList()
                return try {
                    s.split(",").filter { it.trim().isNotEmpty() }.map { it.trim().toInt() }
                } catch (_: Exception) { emptyList() }
            }
        }
        return emptyList()
    }
}
