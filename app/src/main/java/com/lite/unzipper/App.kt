package com.lite.unzipper

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.lite.unzipper.archive.AppContextHolder

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContextHolder.init(this)
        // 启用 Material You 动态色彩：Android 12+ 根据用户壁纸自动取色，
        // Android 11 回退到 themes.xml 中定义的静态色板。
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
