package com.Enco.facefound

import android.app.Application
import android.util.Log

class FaceRecognitionApp : Application() {
    
    companion object {
        private const val TAG = "FaceRecognitionApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 FaceRecognitionApp 初始化")
    }
}