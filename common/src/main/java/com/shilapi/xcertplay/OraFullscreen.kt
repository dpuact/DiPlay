package com.shilapi.xcertplay

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Only the ORA fullscreen APK selects this Application in its manifest. */
class OraFullscreenApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private val focusListeners = mutableMapOf<Activity, ViewTreeObserver.OnWindowFocusChangeListener>()
            private val pending = mutableMapOf<Activity, Runnable>()

            private fun applyAfterLayout(activity: Activity) {
                OraFullscreen.apply(activity)
                val decor = activity.window.decorView
                pending.remove(activity)?.let(decor::removeCallbacks)
                val action = Runnable {
                    pending.remove(activity)
                    if (!activity.isDestroyed && !activity.isFinishing) OraFullscreen.apply(activity)
                }
                pending[activity] = action
                decor.post(action)
            }

            override fun onActivityCreated(activity: Activity, state: Bundle?) {
                val listener = ViewTreeObserver.OnWindowFocusChangeListener { focused ->
                    if (focused && !activity.isDestroyed) OraFullscreen.apply(activity)
                }
                focusListeners[activity] = listener
                activity.window.decorView.viewTreeObserver.addOnWindowFocusChangeListener(listener)
                applyAfterLayout(activity)
            }

            override fun onActivityResumed(activity: Activity) = applyAfterLayout(activity)
            override fun onActivityPaused(activity: Activity) {
                pending.remove(activity)?.let(activity.window.decorView::removeCallbacks)
            }
            override fun onActivityDestroyed(activity: Activity) {
                onActivityPaused(activity)
                focusListeners.remove(activity)?.let { listener ->
                    activity.window.decorView.viewTreeObserver.takeIf { it.isAlive }
                        ?.removeOnWindowFocusChangeListener(listener)
                }
            }
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
        })
    }
}

internal object OraFullscreen {
    fun enabled(context: Context): Boolean = context.applicationContext is OraFullscreenApplication

    fun apply(activity: Activity) {
        if (!enabled(activity)) return
        // Public Android 8.1 APIs only. The manifest separately opts this APK out of
        // split-screen; system-bar hiding alone cannot enlarge a docked activity.
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        activity.window.decorView.requestApplyInsets()
    }
}
