package com.lite.unzipper.archive

import android.content.Context

object AppContextHolder {
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun get(): Context =
        appContext ?: throw IllegalStateException("AppContextHolder 尚未初始化")
}
