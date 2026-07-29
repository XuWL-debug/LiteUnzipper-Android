package com.lite.unzipper.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import com.oplus.animation.OplusViewSeamless

/**
 * OPPO View 无缝动画封装工具类。
 *
 * 仅 ColorOS 16.1+ 且动效等级 B+ 的机型生效，其余机型自动降级为普通跳转。
 * 需要在 build.gradle 中引入 compileOnly("com.oplus.animation:viewseamless:1.0.0@aar")
 */
object OppoViewSeamlessHelper {

    private const val TAG = "OppoSeamless"

    /**
     * 以 View 无缝动画方式启动 Activity。
     *
     * @param view 参与过渡动画的源 View（通常为被点击的卡片或按钮）
     * @param intent 目标 Activity 的 Intent
     * @param cornerRadius View 的圆角半径（dp），有圆角时必传
     * @param bgColor 动画背景兜底色值（不是资源 ID），默认 -1 表示由框架自动取色
     */
    fun startActivityWithSeamless(
        view: View,
        intent: Intent,
        cornerRadius: Float = 0f,
        bgColor: Int = -1
    ) {
        val activity = view.context as? Activity ?: run {
            view.context.startActivity(intent)
            return
        }

        val bundle = Bundle().apply {
            putBoolean(OplusViewSeamless.VIEW_SEAMLESS_OPEN, true)
            if (cornerRadius > 0f) {
                putFloat(OplusViewSeamless.BUNDLE_RADIUS, cornerRadius)
            }
            if (bgColor != -1) {
                putInt(OplusViewSeamless.BUNDLE_COLOR, bgColor)
            }
        }

        val isSupport = try {
            OplusViewSeamless.setSeamlessView(view, activity, bundle, null)
        } catch (e: NoSuchMethodError) {
            Log.d(TAG, "NoSuchMethodError, skip view seamless")
            false
        } catch (e: RuntimeException) {
            Log.d(TAG, "RuntimeException, skip view seamless")
            false
        }

        if (isSupport) {
            activity.startActivity(intent, bundle)
        } else {
            activity.startActivity(intent)
        }
    }

    /**
     * 在需要结束 Activity 前调用，跳过返回无缝动画（改为默认横切过渡）。
     * 适用于返回时源 View 已销毁或位置发生变化的场景。
     */
    fun skipBackAnimation(activity: Activity) {
        try {
            OplusViewSeamless.setSkipViewSeamless(activity)
        } catch (e: NoSuchMethodError) {
            Log.d(TAG, "NoSuchMethodError, skipBackAnim ignored")
        } catch (e: RuntimeException) {
            Log.d(TAG, "RuntimeException, skipBackAnim ignored")
        }
    }
}
