package com.Enco.facefound // 声明包名为人脸识别应用根包

import android.app.Application // 导入Android Application基类
import android.util.Log // 导入Android日志工具

class FaceRecognitionApp : Application() { // 自定义Application类，继承Android Application基类

    companion object { // 伴生对象，存放静态成员
        private const val TAG = "FaceRecognitionApp" // 日志标签常量，用于过滤logcat输出
    }

    override fun onCreate() { // 重写Application的onCreate生命周期方法
        super.onCreate() // 调用父类onCreate完成基类初始化
        Log.d(TAG, "🚀 FaceRecognitionApp 初始化") // 打印调试日志，标记应用启动
    }
}
