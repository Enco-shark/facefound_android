package com.Enco.facerecognition.util // 声明包名为工具模块

import android.content.Context // 导入Android上下文
import android.net.Uri // 导入URI类
import android.util.Log // 导入日志工具
import kotlinx.coroutines.Dispatchers // 导入协程调度器
import kotlinx.coroutines.withContext // 导入withContext切换函数
import java.io.ByteArrayInputStream // 导入字节数组输入流
import java.io.DataInputStream // 导入数据输入流
import java.io.InputStream // 导入输入流基类
import java.nio.ByteBuffer // 导入字节缓冲区
import java.nio.ByteOrder // 导入字节序枚举
import java.util.zip.ZipInputStream // 导入ZIP输入流

object NpzParser { // NPZ解析器单例对象

    private const val TAG = "NpzParser" // 日志标签

    suspend fun parseFromUri(context: Context, uri: Uri): Map<String, FloatArray> = // 从URI解析NPZ文件
        withContext(Dispatchers.IO) { // 在IO线程执行
            try { // 异常保护
                Log.d(TAG, "开始解析文件: $uri") // 打印开始解析日志
                val inputStream = context.contentResolver.openInputStream(uri) // 通过ContentResolver打开输入流
                    ?: throw Exception("无法打开文件: $uri") // 打开失败则抛出异常
                inputStream.use { stream -> parseNpz(stream) } // 使用use自动关闭流，调用核心解析
            } catch (e: Exception) { // 解析异常
                Log.e(TAG, "解析 NPZ 失败: ${e.javaClass.simpleName} - ${e.message}", e) // 打印错误日志
                emptyMap() // 返回空Map
            }
        }

    private fun parseNpz(inputStream: InputStream): Map<String, FloatArray> { // 核心NPZ解析逻辑
        val result = mutableMapOf<String, FloatArray>() // 结果Map
        val zipStream = ZipInputStream(inputStream) // 创建ZIP输入流

        var namesArray: Array<String>? = null // 名字数组
        var embeddingsArray: Array<FloatArray>? = null // 嵌入向量数组
        val rawEntries = mutableMapOf<String, ByteArray>() // 原始条目数据

        try { // 异常保护
            var entry = zipStream.nextEntry // 读取第一个ZIP条目
            while (entry != null) { // 遍历所有条目
                if (!entry.isDirectory && entry.name.endsWith(".npy")) { // 跳过目录，只处理.npy文件
                    try { // 异常保护
                        val data = zipStream.readBytes() // 读取条目全部数据
                        val fileName = entry.name.split("/").last().removeSuffix(".npy") // 提取文件名（去掉路径和扩展名）
                        val hexPreview = data.take(32).joinToString(" ") { "%02X".format(it) } // 取前32字节的十六进制预览
                        Log.d(TAG, "ZIP 条目: $fileName (${data.size} bytes) hex: $hexPreview") // 打印条目信息
                        rawEntries[fileName] = data // 存储原始数据
                    } catch (e: Exception) { // 读取异常
                        Log.e(TAG, "读取条目 ${entry.name} 失败", e) // 打印错误日志
                    }
                }
                zipStream.closeEntry() // 关闭当前条目
                entry = zipStream.nextEntry // 读取下一个条目
            }
        } finally { // 清理资源
            try {
                zipStream.close() // 关闭ZIP流
            } catch (e: Exception) {
                Log.w(TAG, "关闭 ZipInputStream 失败: ${e.message}") // 关闭失败警告
            }
        }

        if (rawEntries.isEmpty()) { // 没有找到.npy文件
            Log.e(TAG, "ZIP 中没有找到 .npy 文件") // 打印错误
            Log.e(TAG, "提示: 确认文件是有效的 NPZ 格式（ZIP 包含 .npy 文件）") // 打印提示
            return emptyMap() // 返回空Map
        }

        Log.i(TAG, "rawEntries 包含 ${rawEntries.size} 个条目: ${rawEntries.keys.joinToString(", ")}") // 打印条目列表

        // 查找备选名字 key (names / labels / name_list / template_names 等)
        val nameKeyCandidates = listOf("names", "labels", "name_list", "template_names", "ids", "person_ids", "label") // 名字键候选列表
        val embeddingKeyCandidates = listOf("embeddings", "features", "templates", "template_data", "embs", "vectors") // 嵌入键候选列表

        val actualNameKey = nameKeyCandidates.firstOrNull { rawEntries.containsKey(it) } // 找到实际存在的名字键
        val actualEmbKey = embeddingKeyCandidates.firstOrNull { rawEntries.containsKey(it) } // 找到实际存在的嵌入键

        // 如果用的是备选 key 但与 "names"/"embeddings" 不同，做映射
        if ((actualNameKey != null && actualNameKey != "names") || (actualEmbKey != null && actualEmbKey != "embeddings")) { // 需要映射
            if (actualNameKey != null && actualNameKey != "names") { // 名字键需要映射
                rawEntries["names"] = rawEntries.remove(actualNameKey)!! // 重命名为"names"
                Log.i(TAG, "将 '$actualNameKey' 映射为 'names'") // 打印映射日志
            }
            if (actualEmbKey != null && actualEmbKey != "embeddings") { // 嵌入键需要映射
                rawEntries["embeddings"] = rawEntries.remove(actualEmbKey)!! // 重命名为"embeddings"
                Log.i(TAG, "将 '$actualEmbKey' 映射为 'embeddings'") // 打印映射日志
            }
        }

        // 尝试解析 embeddings（无论是否有 names）
        val hasEmbeddingsOnly = rawEntries.containsKey("embeddings") && !rawEntries.containsKey("names") // 仅有嵌入数据
        val hasNamesOnly = rawEntries.containsKey("names") && !rawEntries.containsKey("embeddings") // 仅有名字数据

        if (hasEmbeddingsOnly) { // 仅有嵌入数据
            Log.i(TAG, "仅有 embeddings，无 names，使用索引名称") // 打印提示
            embeddingsArray = parseEmbeddingsNpy(rawEntries["embeddings"]!!) // 解析嵌入数据
            if (embeddingsArray != null && embeddingsArray.isNotEmpty()) { // 解析成功
                for (i in embeddingsArray.indices) { // 遍历每个嵌入
                    result["Person_$i"] = embeddingsArray[i] // 使用索引作为名字
                }
                Log.i(TAG, "✅ 索引名称回退: ${result.size} 个模板 (Person_0 ~ Person_${result.size - 1})") // 打印结果
                return result // 返回结果
            }
            Log.w(TAG, "embeddings 解析失败，继续尝试其他策略") // 解析失败，继续
        }

        if (hasNamesOnly) { // 仅有名字数据
            Log.i(TAG, "仅有 names，无 embeddings 数据，无法创建模板") // 打印提示
            return emptyMap() // 返回空Map
        }

        // 桌面版格式: names + embeddings
        if (rawEntries.containsKey("names") && rawEntries.containsKey("embeddings")) { // 标准桌面版格式
            Log.i(TAG, "检测到 names+embeddings 桌面版格式，开始解析...") // 打印提示
            namesArray = parseNamesNpy(rawEntries["names"]!!) // 解析名字数据
            embeddingsArray = parseEmbeddingsNpy(rawEntries["embeddings"]!!) // 解析嵌入数据

            if (namesArray == null) { // 名字解析失败
                Log.w(TAG, "names 解析失败，将使用索引名称") // 打印警告
            }
            if (embeddingsArray == null) { // 嵌入解析失败
                Log.w(TAG, "embeddings 解析失败，将尝试单文件格式") // 打印警告
            }

            if (namesArray != null && embeddingsArray != null && namesArray.size == embeddingsArray.size) { // 两者都成功且数量匹配
                val duplicateNames = namesArray.groupBy { it }.filter { it.value.size > 1 }.keys // 查找重复名字
                if (duplicateNames.isNotEmpty()) { // 有重复名字
                    Log.w(TAG, "发现 ${duplicateNames.size} 个重复名字，将添加索引后缀: ${duplicateNames.take(5).joinToString()}") // 打印警告
                }
                namesArray.zip(embeddingsArray).forEachIndexed { idx, (name, embedding) -> // 配对名字和嵌入
                    val key = if (duplicateNames.contains(name)) "${name}_$idx" else name // 重复名字添加索引后缀
                    result[key] = embedding // 添加到结果
                }
                val sampleNames = result.keys.take(5).joinToString(", ") // 取前5个名字作为样本
                val sampleDim = result.values.firstOrNull()?.size ?: 0 // 获取嵌入维度
                Log.i(TAG, "桌面版格式解析完成: ${result.size} 个模板 (dim=$sampleDim, 样本: $sampleNames)") // 打印结果
                return result // 返回结果
            }

            if (embeddingsArray != null && embeddingsArray.isNotEmpty()) { // 嵌入解析成功但名字不匹配
                val useNames = namesArray ?: Array(embeddingsArray.size) { "Person_$it" } // 使用索引名作为回退
                val count = minOf(useNames.size, embeddingsArray.size) // 取较小值
                for (i in 0 until count) { // 遍历
                    result[useNames[i]] = embeddingsArray[i] // 添加到结果
                }
                if (namesArray == null) { // 名字为空
                    Log.i(TAG, "⚠️ 使用索引名称回退: ${result.size} 个模板 (Person_0 ~ Person_${count - 1})") // 打印回退提示
                } else { // 名字数量不匹配
                    Log.i(TAG, "⚠️ 数量不匹配回退: ${result.size} 个模板") // 打印回退提示
                }
                return result // 返回结果
            }

            Log.w(TAG, "names(${namesArray?.size ?: 0}) 和 embeddings(${embeddingsArray?.size ?: 0}) 不匹配，尝试单文件") // 打印警告
        } else { // 非标准格式
            Log.i(TAG, "未检测到标准 names+embeddings，尝试单文件格式解析") // 打印提示
        }

        // 单文件格式: 每个 .npy 文件是一个人脸模板
        var singleFileCount = 0 // 单文件模板计数
        rawEntries.forEach { (fileName, data) -> // 遍历所有条目
            if (fileName == "names" || fileName == "embeddings") return@forEach // 跳过names和embeddings
            try { // 异常保护
                val embedding = parseNpy(ByteArrayInputStream(data)) // 解析单个npy文件
                if (embedding != null && embedding.isNotEmpty()) { // 解析成功
                    result[fileName] = embedding // 使用文件名作为模板名
                    singleFileCount++ // 递增计数
                    Log.d(TAG, "单文件模板: $fileName (${embedding.size} 维)") // 打印解析结果
                } else { // 解析返回空
                    Log.w(TAG, "单文件 $fileName 解析返回空") // 打印警告
                }
            } catch (e: Exception) { // 解析异常
                Log.e(TAG, "解析 $fileName 失败: ${e.message}") // 打印错误
            }
        }
        Log.i(TAG, "单文件格式解析: $singleFileCount 个模板") // 打印单文件解析结果

        // 如果单文件也没解析出来，回退：对 names+embeddings 组合
        if (result.isEmpty() && namesArray != null && embeddingsArray != null) { // 结果为空但有原始数据
            val count = minOf(namesArray.size, embeddingsArray.size) // 取较小值
            for (i in 0 until count) { // 遍历
                result[namesArray[i]] = embeddingsArray[i] // 直接配对
            }
            Log.i(TAG, "回退解析完成: ${result.size} 个模板") // 打印回退结果
        }

        // == 最后防线: raw float32 暴力回退 ==
        if (result.isEmpty()) { // 所有标准策略都失败
            Log.w(TAG, "所有标准策略均失败，进入 raw float32 暴力回退...") // 打印提示
            for ((fileName, data) in rawEntries) { // 遍历所有条目
                try { // 异常保护
                    val (_, rawFloats) = tryRawFloat32Fallback(ByteArrayInputStream(data)) // 尝试raw float32解析
                    if (rawFloats != null && rawFloats.isNotEmpty()) { // 解析成功
                        // 扔给暴力维度猜测
                        val embeds = parseEmbeddingsNpy(data) ?: arrayOf(rawFloats) // 尝试解析为嵌入数组，回退为单个
                        embeds.forEachIndexed { idx, emb -> // 遍历嵌入
                            val name = if (embeds.size == 1) fileName else "${fileName}_$idx" // 单个用文件名，多个加索引
                            result[name] = emb // 添加到结果
                        }
                        Log.i(TAG, "raw 回退从 '$fileName' 解析出 ${embeds.size} 个模板") // 打印结果
                    }
                } catch (_: Exception) {} // 忽略异常
            }
        }

        if (result.isEmpty()) { // 彻底失败
            Log.e(TAG, "❌ NPZ 解析彻底失败: 含 raw 回退在内的所有策略均未解析出模板") // 打印最终错误
            Log.e(TAG, "调试信息 - rawEntries keys: ${rawEntries.keys.joinToString(", ")}") // 打印调试信息
            if (namesArray != null) Log.e(TAG, "names 解析结果: ${namesArray.size} 个名字") // 打印names解析结果
            if (embeddingsArray != null) Log.e(TAG, "embeddings 解析结果: ${embeddingsArray.size} 个嵌入向量") // 打印embeddings解析结果
        } else { // 解析成功
            Log.i(TAG, "✅ NPZ 解析成功，共 ${result.size} 个模板") // 打印成功日志
            for (name in result.keys) { // 遍历所有模板
                Log.d(TAG, "  模板: $name (${result[name]?.size ?: 0} 维)") // 打印每个模板信息
            }
        }
        return result // 返回结果
    }

    private fun parseNamesNpy(data: ByteArray): Array<String>? { // 解析names.npy文件
        return try { // 异常保护
            val inputStream = DataInputStream(ByteArrayInputStream(data)) // 创建数据输入流

            val magic = ByteArray(6) // 魔数缓冲区（6字节）
            inputStream.readFully(magic) // 读取魔数
            if (magic[0] != 0x93.toByte() || magic[1] != 'N'.code.toByte() || // 验证NPY魔数
                magic[2] != 'U'.code.toByte() || magic[3] != 'M'.code.toByte() ||
                magic[4] != 'P'.code.toByte() || magic[5] != 'Y'.code.toByte()
            ) {
                Log.w(TAG, "names.npy 魔数异常") // 魔数不匹配警告
            }

            val majorVersion = inputStream.readUnsignedByte() // 读取主版本号
            inputStream.readUnsignedByte() // 读取次版本号（忽略）
            val headerLen = readHeaderLen(inputStream, majorVersion) // 读取header长度

            val headerBytes = ByteArray(headerLen) // header字节缓冲区
            inputStream.readFully(headerBytes) // 读取header
            val header = String(headerBytes, Charsets.UTF_8) // 转为UTF-8字符串
            Log.d(TAG, "names header: ${header.take(100)}") // 打印header前100字符

            val dtype = extractDtype(header) // 从header提取dtype
            val shape = extractShape(header) // 从header提取shape
            val count = if (shape.isNotEmpty()) shape[0] else 0 // 获取元素数量
            Log.d(TAG, "names header shape=$shape, dtype=$dtype, 预期数量: $count") // 打印解析信息

            val remaining = inputStream.readBytes() // 读取剩余数据
            inputStream.close() // 关闭流

            val unicodeResult = tryParseUnicodeStrings(remaining, dtype, count) // 尝试Unicode解析策略
            if (unicodeResult != null && unicodeResult.size == count && count > 0) { // Unicode解析成功
                Log.d(TAG, "Unicode 策略解析到 ${unicodeResult.size} 个名字") // 打印结果
                return unicodeResult.toTypedArray() // 返回结果
            }

            val result = parseNamesData(remaining, count) // 使用其他策略解析
            Log.d(TAG, "解析到 ${result.size} 个名字") // 打印结果
            result.toTypedArray() // 转为数组返回

        } catch (e: Exception) { // 解析异常
            Log.e(TAG, "解析 names.npy 失败: ${e.message}", e) // 打印错误
            null // 返回null
        }
    }

    private fun tryParseUnicodeStrings(data: ByteArray, dtype: String, expectedCount: Int): List<String>? { // 尝试Unicode字符串解析
        val charSizeMatch = Regex("<U(\\d+)").find(dtype) // 匹配dtype中的Unicode字符大小
        if (charSizeMatch == null) return null // 不匹配则返回null
        val charSize = charSizeMatch.groupValues[1].toIntOrNull() ?: return null // 解析字符大小
        if (charSize <= 0 || charSize > 256) return null // 字符大小不合理则返回null

        if (expectedCount <= 0) return null // 预期数量无效则返回null

        val bytesPerElement = charSize * 4 // 每个元素的字节数（UTF-32每个字符4字节）
        val neededBytes = expectedCount * bytesPerElement // 所需总字节数
        if (data.size < neededBytes) { // 数据不足
            Log.w(TAG, "Unicode 解析: 数据不足 (需要 $neededBytes, 实际 ${data.size})") // 打印警告
            return null // 返回null
        }

        val result = mutableListOf<String>() // 结果列表
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN) // 包装为小端字节序缓冲区

        for (i in 0 until expectedCount) { // 遍历每个元素
            val offset = i * bytesPerElement // 当前元素偏移
            val sb = StringBuilder() // 字符串构建器
            for (c in 0 until charSize) { // 遍历每个字符
                val codePoint = buf.getInt(offset + c * 4) // 读取UTF-32码点
                if (codePoint == 0) break // 遇到空字符停止
                sb.appendCodePoint(codePoint) // 追加码点
            }
            result.add(sb.toString()) // 添加到结果
        }

        val nonEmpty = result.count { it.isNotBlank() } // 统计非空字符串数量
        Log.d(TAG, "Unicode<$charSize> 解析: ${result.size} 个, 非空 $nonEmpty 个") // 打印统计
        if (nonEmpty < expectedCount * 0.5) return null // 非空率低于50%则认为解析失败

        return result // 返回结果
    }

    /**
     * raw float32 回退: 将剩余字节解释为 float32 little-endian
     */
    private fun tryRawFloat32Fallback(inputStream: InputStream): Pair<List<Int>, FloatArray?> { // raw float32暴力解析
        return try { // 异常保护
            val remaining = inputStream.readBytes() // 读取所有字节
            if (remaining.size < 4) { // 不足4字节
                Log.w(TAG, "raw 回退: 数据不足 (${remaining.size} bytes)") // 打印警告
                return Pair(emptyList(), null) // 返回空
            }
            // 对齐到 4 字节边界
            val alignedSize = remaining.size - (remaining.size % 4) // 对齐到4字节
            if (alignedSize < 4) return Pair(emptyList(), null) // 不足4字节返回空

            val count = alignedSize / 4 // 浮点数数量
            val buf = ByteBuffer.wrap(remaining, 0, alignedSize).order(ByteOrder.LITTLE_ENDIAN) // 包装为小端缓冲区
            val result = FloatArray(count) // 结果数组
            for (i in 0 until count) result[i] = buf.float // 逐个读取浮点数

            // 检查合理性：不应全是 NaN/Inf
            val validCount = result.count { it.isFinite() } // 统计有效浮点数
            if (validCount < count * 0.5f) { // 有效率低于50%
                Log.w(TAG, "raw 回退: 仅 $validCount/$count 个有效浮点数，数据可能不是 float32") // 打印警告
                return Pair(listOf(count), null) // 返回null
            }

            Log.i(TAG, "raw float32 回退成功: $count 个浮点数") // 打印成功日志
            Pair(listOf(count), result) // 返回结果
        } catch (e: Exception) { // 异常
            Log.e(TAG, "raw 回退失败: ${e.message}") // 打印错误
            Pair(emptyList(), null) // 返回空
        }
    }

    private fun parseNamesData(data: ByteArray, expectedCount: Int): List<String> { // 多策略名字解析
        // 策略1: Python numpy object array pickle 格式
        val pickle = tryParsePickleStrings(data, expectedCount) // 尝试pickle解析
        if (pickle.size >= expectedCount && expectedCount > 0) return pickle // 数量匹配则返回
        if (pickle.isNotEmpty() && expectedCount == 0) return pickle // 无预期数量时有结果就返回

        // 策略2: 指针数组 + 字符串
        val pointer = tryParsePointerStrings(data, expectedCount) // 尝试指针解析
        if (pointer.size >= expectedCount && expectedCount > 0) return pointer // 数量匹配则返回

        // 策略3: 扫描所有可读 UTF-8 字符串
        val scanned = scanReadableStrings(data, expectedCount) // 尝试扫描解析
        if (scanned.isNotEmpty()) return scanned // 有结果就返回

        // 策略4: null-terminated 字符串
        val nullTerm = tryParseNullTermStrings(data, expectedCount) // 尝试null终止解析
        if (nullTerm.isNotEmpty()) return nullTerm // 有结果就返回

        return pickle.ifEmpty { pointer.ifEmpty { scanned } } // 返回第一个非空结果
    }

    private fun tryParsePickleStrings(data: ByteArray, expectedCount: Int): List<String> { // pickle格式字符串解析
        val result = mutableListOf<String>() // 结果列表
        try { // 异常保护
            var i = 0 // 当前位置
            while (i < data.size && (expectedCount == 0 || result.size < expectedCount)) { // 遍历数据
                when (data[i].toInt() and 0xFF) { // 读取当前字节作为操作码
                    0x55 -> { // SHORT_BINUNICODE (pickle protocol 2+)
                        if (i + 2 <= data.size) { // 确保有足够数据
                            val len = ((data[i + 1].toInt() and 0xFF) or // 读取长度低字节
                                      ((data[i + 2].toInt() and 0xFF) shl 8)) // 读取长度高字节
                            if (len in 1..1024 && i + 3 + len <= data.size) { // 长度合理且数据足够
                                val str = String(data, i + 3, len, Charsets.UTF_8) // 解码UTF-8字符串
                                if (str.isNotBlank() && str.all { c -> !c.isISOControl() || c == ' ' }) { // 非空且无控制字符
                                    result.add(str) // 添加到结果
                                }
                            }
                            i += 3 + len // 跳过已处理数据
                            continue // 继续下一个
                        }
                    }
                    0x8C -> { // SHORT_BINUNICODE (pickle protocol 4+)
                        if (i + 1 < data.size) { // 确保有足够数据
                            val len = data[i + 1].toInt() and 0xFF // 读取长度（单字节）
                            if (len in 1..255 && i + 2 + len <= data.size) { // 长度合理且数据足够
                                val str = String(data, i + 2, len, Charsets.UTF_8) // 解码UTF-8字符串
                                if (str.isNotBlank() && str.all { c -> !c.isISOControl() || c == ' ' }) { // 非空且无控制字符
                                    result.add(str) // 添加到结果
                                }
                            }
                            i += 2 + len // 跳过已处理数据
                            continue // 继续下一个
                        }
                    }
                    0x8D -> { // BINUNICODE (pickle protocol 4+)
                        if (i + 5 <= data.size) { // 确保有足够数据
                            val len = ByteBuffer.wrap(data, i + 1, 4) // 读取4字节长度
                                .order(ByteOrder.LITTLE_ENDIAN).int // 小端字节序
                            if (len in 1..1024 && i + 5 + len <= data.size) { // 长度合理且数据足够
                                val str = String(data, i + 5, len, Charsets.UTF_8) // 解码UTF-8字符串
                                if (str.isNotBlank() && str.all { c -> !c.isISOControl() || c == ' ' }) { // 非空且无控制字符
                                    result.add(str) // 添加到结果
                                }
                            }
                            i += 5 + len // 跳过已处理数据
                            continue // 继续下一个
                        }
                    }
                }
                i++ // 移动到下一个字节
            }
            Log.d(TAG, "pickle 策略解析到 ${result.size} 个字符串") // 打印结果
        } catch (_: Exception) {} // 忽略异常
        return result // 返回结果
    }

    private fun tryParsePointerStrings(data: ByteArray, expectedCount: Int): List<String> { // 指针数组解析策略
        val result = mutableListOf<String>() // 结果列表
        try { // 异常保护
            if (data.size < expectedCount * 8 || expectedCount <= 0) return result // 数据不足或数量无效

            val pointers = mutableListOf<Int>() // 指针列表
            for (i in 0 until expectedCount) { // 遍历每个指针
                val offset = i * 8 // 指针偏移（8字节）
                val ptr = ByteBuffer.wrap(data, offset, 8) // 读取8字节
                    .order(ByteOrder.LITTLE_ENDIAN).long.toInt() // 转为整数
                if (ptr in 0 until data.size) { // 指针在有效范围内
                    pointers.add(ptr) // 添加到指针列表
                }
            }

            if (pointers.size != expectedCount) return result // 指针数量不匹配

            for (ptr in pointers) { // 遍历每个指针
                val strLen = ByteBuffer.wrap(data, ptr, 8) // 读取字符串长度
                    .order(ByteOrder.LITTLE_ENDIAN).long.toInt() // 转为整数
                if (strLen in 1..1024 && ptr + 8 + strLen <= data.size) { // 长度合理且数据足够
                    val str = String(data, ptr + 8, strLen, Charsets.UTF_8) // 解码字符串
                    if (str.isNotBlank()) result.add(str) // 非空则添加
                }
            }
            Log.d(TAG, "pointer 策略解析到 ${result.size} 个字符串") // 打印结果
        } catch (_: Exception) {} // 忽略异常
        return result // 返回结果
    }

    private fun scanReadableStrings(data: ByteArray, expectedCount: Int): List<String> { // 扫描可读字符串策略
        val result = mutableListOf<String>() // 结果列表
        try { // 异常保护
            var start = -1 // 当前字符串起始位置
            for (i in data.indices) { // 遍历每个字节
                val b = data[i].toInt() and 0xFF // 读取字节值
                // ✅ P2-5 修复：UTF-8 起始字节判断
                //   原 b >= 192 会匹配 0xC0-0xFF，但 0xC0-0xC1 是无效 UTF-8 起始，0xF8-0xFF 是非法字节。
                //   正确范围：ASCII 可打印 0x20-0x7E，UTF-8 多字节起始 0xC2-0xF4（2/3/4 字节序列）。
                //   0x80-0xBF 是 UTF-8 续接字节，不应单独作为字符串起始。
                val isUtf8Start = b in 0xC2..0xF4 // 合法 UTF-8 多字节起始字节
                if (b in 32..126 || isUtf8Start) { // ASCII可打印字符或合法UTF-8多字节起始
                    if (start < 0) start = i // 记录起始位置
                } else { // 不可打印字符
                    if (start >= 0) { // 有正在收集的字符串
                        val len = i - start // 字符串长度
                        if (len in 2..128) { // 长度合理
                            try { // 异常保护
                                val str = String(data, start, len, Charsets.UTF_8) // 解码字符串
                                if (str.isNotBlank() && str.none { it.isISOControl() && it != ' ' }) { // 非空且无控制字符
                                    result.add(str) // 添加到结果
                                    if (expectedCount > 0 && result.size >= expectedCount) return result // 达到预期数量
                                }
                            } catch (_: Exception) {} // 忽略解码异常
                        }
                        start = -1 // 重置起始位置
                    }
                }
            }
            Log.d(TAG, "scan 策略解析到 ${result.size} 个字符串") // 打印结果
        } catch (_: Exception) {} // 忽略异常
        return result // 返回结果
    }

    private fun tryParseNullTermStrings(data: ByteArray, expectedCount: Int): List<String> { // null终止字符串解析策略
        val result = mutableListOf<String>() // 结果列表
        try { // 异常保护
            var offset = 0 // 当前偏移
            while (offset < data.size && (expectedCount == 0 || result.size < expectedCount)) { // 遍历数据
                var zeroPos = -1 // null字符位置
                for (i in offset until data.size) { // 从当前位置搜索null
                    if (data[i] == 0.toByte()) { // 找到null
                        zeroPos = i // 记录位置
                        break // 退出搜索
                    }
                }
                if (zeroPos > offset) { // 找到有效字符串
                    val str = String(data, offset, zeroPos - offset, Charsets.UTF_8) // 解码字符串
                        .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "") // 移除控制字符
                    if (str.isNotBlank() && str.length > 1) { // 非空且长度大于1
                        result.add(str) // 添加到结果
                    }
                    offset = zeroPos + 1 // 移动到null之后
                } else { // 未找到null
                    break // 退出循环
                }
            }
            Log.d(TAG, "null-term 策略解析到 ${result.size} 个字符串") // 打印结果
        } catch (_: Exception) {} // 忽略异常
        return result // 返回结果
    }

    private fun parseEmbeddingsNpy(data: ByteArray): Array<FloatArray>? { // 解析embeddings.npy
        return try { // 异常保护
            val (shape, flatArray) = parseNpyWithShape(ByteArrayInputStream(data)) // 解析NPY获取shape和数据
            if (flatArray == null || flatArray.isEmpty()) return null // 数据为空则返回null

            if (shape.size == 2 && shape[1] > 0 && shape[0] > 0) { // 二维数组（标准格式）
                val dim = shape[1] // 嵌入维度
                val count = shape[0] // 嵌入数量
                Log.d(TAG, "embeddings: $count x $dim (从 header 读取)") // 打印shape信息
                val result = Array(count) { i -> flatArray.copyOfRange(i * dim, (i + 1) * dim) } // 按维度拆分
                for (i in result.indices) { // 遍历每个嵌入
                    l2Normalize(result[i]) // L2归一化
                }
                Log.d(TAG, "embeddings 已 L2 归一化, 样本[0]前5: ${result[0].take(5).joinToString(",") { "%.4f".format(it) }}") // 打印样本
                return result // 返回结果
            }

            val commonDims = listOf(512, 256, 128, 1024, 768, 2560, 320, 2048) // 常见嵌入维度列表
            for (dim in commonDims) { // 遍历常见维度
                if (flatArray.size % dim == 0 && flatArray.size >= dim) { // 能被整除
                    val count = flatArray.size / dim // 计算数量
                    Log.i(TAG, "embeddings 暴力猜测: $count x $dim") // 打印猜测结果
                    val result = Array(count) { i -> flatArray.copyOfRange(i * dim, (i + 1) * dim) } // 按维度拆分
                    for (i in result.indices) { // 遍历每个嵌入
                        l2Normalize(result[i]) // L2归一化
                    }
                    return result // 返回结果
                }
            }

            Log.i(TAG, "embeddings 作为单个 ${flatArray.size} 维嵌入") // 无法拆分，作为单个嵌入
            arrayOf(flatArray) // 返回单元素数组
        } catch (e: Exception) { // 解析异常
            Log.e(TAG, "解析 embeddings 失败: ${e.message}", e) // 打印错误
            null // 返回null
        }
    }

    /**
     * 一次读取完成 header 解析和 float 数据提取，避免重复 IO
     * @return Pair(shape, floatArray)
     */
    private fun parseNpyWithShape(inputStream: InputStream): Pair<List<Int>, FloatArray?> { // 解析NPY文件获取shape和数据
        return try { // 异常保护
            val dataInput = DataInputStream(inputStream) // 创建数据输入流
            val magic = ByteArray(6) // 魔数缓冲区
            dataInput.readFully(magic) // 读取魔数

            if (magic[0] != 0x93.toByte() || magic[1] != 'N'.code.toByte() || // 验证NPY魔数
                magic[2] != 'U'.code.toByte() || magic[3] != 'M'.code.toByte() ||
                magic[4] != 'P'.code.toByte() || magic[5] != 'Y'.code.toByte()
            ) {
                Log.w(TAG, "NPY 魔数异常: ${magic.joinToString("") { "%02X".format(it) }}, 尝试 raw float32 回退") // 魔数异常
                dataInput.close() // 关闭流
                return tryRawFloat32Fallback(inputStream) // 回退到raw解析
            }

            val majorVersion = dataInput.readUnsignedByte() // 读取主版本号
            dataInput.readUnsignedByte() // 读取次版本号（忽略）
            val headerLen = readHeaderLen(dataInput, majorVersion) // 读取header长度
            val headerBytes = ByteArray(headerLen) // header缓冲区
            dataInput.readFully(headerBytes) // 读取header
            val header = String(headerBytes, Charsets.UTF_8) // 转为字符串
            Log.d(TAG, "NPY header: ${header.take(150)}") // 打印header前150字符

            val dtype = extractDtype(header) // 提取dtype
            val shape = extractShape(header) // 提取shape

            if (shape.isEmpty()) { // shape为空
                Log.w(TAG, "NPY shape 为空") // 打印警告
                dataInput.close() // 关闭流
                return Pair(emptyList(), null) // 返回空
            }

            val totalElements = shape.fold(1L) { acc, i -> acc * i } // 计算总元素数
            if (totalElements > 10_000_000L) { // 元素数过多
                Log.w(TAG, "NPY 数组过大: $totalElements 个元素 (限制10M)，跳过") // 打印警告
                dataInput.close() // 关闭流
                return Pair(shape, null) // 返回null
            }
            if (totalElements <= 0) { // 元素数为0
                Log.w(TAG, "NPY 元素数为 0") // 打印警告
                dataInput.close() // 关闭流
                return Pair(shape, null) // 返回null
            }

            val count = totalElements.toInt() // 转为Int
            val result = when { // 根据dtype选择解析方式
                dtype.contains("f4") || dtype.contains("float32") -> readFloat32Batch(dataInput, count) // float32
                dtype.contains("f8") || dtype.contains("float64") -> readFloat64Batch(dataInput, count) // float64
                dtype.contains("i4") || dtype.contains("int32") -> readToFloatBatch(dataInput, count, 4) { buf -> buf.int.toFloat() } // int32转float
                dtype.contains("i8") || dtype.contains("int64") -> readToFloatBatch(dataInput, count, 8) { buf -> buf.long.toFloat() } // int64转float
                else -> { // 未知dtype
                    Log.w(TAG, "未知 dtype '$dtype'，尝试 float32 回退") // 打印警告
                    readFloat32Batch(dataInput, count) // 回退到float32
                }
            }

            dataInput.close() // 关闭流
            Log.d(TAG, "NPY 解析完成: ${result.size} 个 float, shape=$shape, dtype=$dtype") // 打印完成信息
            Pair(shape, result) // 返回shape和数据

        } catch (e: Exception) { // 解析异常
            Log.e(TAG, "NPY 解析异常: ${e.javaClass.simpleName} - ${e.message}") // 打印错误
            Pair(emptyList(), null) // 返回空
        }
    }

    private fun parseNpy(inputStream: InputStream): FloatArray? { // 解析单个NPY文件
        val (_, result) = parseNpyWithShape(inputStream) // 调用带shape的解析
        return result // 返回数据
    }

    private fun readFloat32Batch(input: DataInputStream, count: Int): FloatArray { // 批量读取float32
        val result = FloatArray(count) // 结果数组
        val buf = ByteArray(count * 4) // 字节缓冲区
        input.readFully(buf) // 读取全部字节
        val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN) // 包装为小端缓冲区
        for (i in 0 until count) result[i] = bb.float // 逐个读取
        return result // 返回结果
    }

    private fun readFloat64Batch(input: DataInputStream, count: Int): FloatArray { // 批量读取float64转float32
        val result = FloatArray(count) // 结果数组
        val buf = ByteArray(count * 8) // 字节缓冲区
        input.readFully(buf) // 读取全部字节
        val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN) // 包装为小端缓冲区
        for (i in 0 until count) result[i] = bb.double.toFloat() // 逐个读取并转为float32
        return result // 返回结果
    }

    private fun readToFloatBatch(input: DataInputStream, count: Int, elemSize: Int, convert: (ByteBuffer) -> Float): FloatArray { // 通用批量读取
        val result = FloatArray(count) // 结果数组
        val buf = ByteArray(count * elemSize) // 字节缓冲区
        input.readFully(buf) // 读取全部字节
        val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN) // 包装为小端缓冲区
        var offset = 0 // 当前偏移
        for (i in 0 until count) { // 遍历每个元素
            bb.position(offset) // 设置读取位置
            result[i] = convert(bb) // 使用转换函数读取
            offset += elemSize // 移动偏移
        }
        return result // 返回结果
    }

    private fun readHeaderLen(input: DataInputStream, majorVersion: Int): Int { // 读取NPY header长度
        return when (majorVersion) { // 根据版本号
            1 -> { // 版本1.x
                val b = ByteArray(2); input.readFully(b) // 读取2字节
                ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).short.toInt() // 转为整数
            }
            2, 3 -> { // 版本2.x或3.x
                val b = ByteArray(4); input.readFully(b) // 读取4字节
                ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).int // 转为整数
            }
            else -> throw Exception("不支持的 NPY 版本: $majorVersion") // 不支持的版本
        }
    }

    private fun l2Normalize(vector: FloatArray) { // L2归一化（原地修改）
        var sum = 0f // 平方和
        for (v in vector) sum += v * v // 累加平方
        val norm = kotlin.math.sqrt(sum) // 计算L2范数
        if (norm > 0f) { // 范数大于0
            for (i in vector.indices) vector[i] /= norm // 每个元素除以范数
        }
    }

    private fun extractDtype(header: String): String { // 从NPY header提取dtype
        val patterns = listOf( // 正则表达式列表
            "'descr':\\s*'([^']+)'".toRegex(), // 单引号格式
            "\"descr\":\\s*\"([^\"]+)\"".toRegex() // 双引号格式
        )
        for (p in patterns) { // 遍历正则
            val m = p.find(header) // 匹配
            if (m != null) return m.groupValues[1] // 返回捕获组
        }
        return "<f4" // 默认返回float32
    }

    private fun extractShape(header: String): List<Int> { // 从NPY header提取shape
        val patterns = listOf( // 正则表达式列表
            "'shape':\\s*\\(([^)]*)\\)".toRegex(), // 单引号圆括号格式
            "\"shape\":\\s*\\(([^)]*)\\)".toRegex(), // 双引号圆括号格式
            "'shape':\\s*\\[([^]]*)\\]".toRegex() // 单引号方括号格式
        )
        for (p in patterns) { // 遍历正则
            val m = p.find(header) // 匹配
            if (m != null) { // 找到匹配
                val s = m.groupValues[1].trim() // 提取并去除空白
                if (s.isEmpty()) return emptyList() // 空则返回空列表
                return try { // 尝试解析
                    s.split(",").filter { it.trim().isNotEmpty() }.map { it.trim().toInt() } // 按逗号分割转整数
                } catch (_: Exception) { emptyList() } // 解析失败返回空
            }
        }
        return emptyList() // 未找到返回空
    }
}
