package com.shilapi.xcertplay

import android.app.Activity
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.shilapi.xcertplay.host.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27, 28], application = OraFullscreenApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class OraFullscreenTest {
    private val mask = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

    private fun assertFullscreen(activity: Activity) {
        assertTrue(OraFullscreen.enabled(activity))
        assertEquals(mask, activity.window.decorView.systemUiVisibility and mask)
        assertNotEquals(0, activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    @Test fun creationAndResumeOverrideAnActivityThatRestoresSystemBars() {
        val controller = Robolectric.buildActivity(ResettingActivity::class.java).create().start().resume().visible()
        shadowOf(Looper.getMainLooper()).idle()
        assertFullscreen(controller.get())
        controller.pause().stop().destroy()
    }

    @Test fun returningFromAnotherWindowRestoresImmersiveMode() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = controller.get()
        activity.window.decorView.systemUiVisibility = 0
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        ReflectionHelpers.callInstanceMethod<Void>(activity.window.decorView.viewTreeObserver,
            "dispatchOnWindowFocusChange", ReflectionHelpers.ClassParameter.from(Boolean::class.javaPrimitiveType, true))
        assertFullscreen(activity)
        controller.pause().stop().destroy()
    }

    @Test fun destroyedActivityDoesNotReceiveDeferredFullscreenChanges() {
        val controller = Robolectric.buildActivity(Activity::class.java).create().start().resume()
        val activity = controller.get()
        controller.pause().stop().destroy()
        activity.window.decorView.systemUiVisibility = 0
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, activity.window.decorView.systemUiVisibility)
    }

    @Test fun savedVisibleBarsCannotDisableFullscreenOrChangeAudioSettings() {
        val activity = Robolectric.buildActivity(CarPlayHostActivity::class.java).get()
        AirPlayPersistence.saveHideTopBar(activity, false)
        AirPlayPersistence.saveHideBottomBar(activity, false)
        AirPlayPersistence.saveAudioFocusEnabled(activity, true)
        AirPlayPersistence.saveNavigationAudioFocusEnabled(activity, true)
        AirPlayPersistence.saveSiriUsesNavigation(activity, true)
        activity.javaClass.getDeclaredMethod("loadPersistedSettings").apply { isAccessible = true }.invoke(activity)
        assertEquals(true, ReflectionHelpers.getField<Boolean>(activity, "hideTopBar"))
        assertEquals(true, ReflectionHelpers.getField<Boolean>(activity, "hideBottomBar"))
        activity.javaClass.getDeclaredMethod("applyFullscreenMode").apply { isAccessible = true }.invoke(activity)
        assertFullscreen(activity)
        assertTrue(AirPlayPersistence.loadAudioFocusEnabled(activity))
        assertTrue(AirPlayPersistence.loadNavigationAudioFocusEnabled(activity))
        assertTrue(AirPlayPersistence.loadSiriUsesNavigation(activity))
        // Reading the policy does not destroy the user's old display preferences.
        assertFalse(AirPlayPersistence.loadHideTopBar(activity))
        assertFalse(AirPlayPersistence.loadHideBottomBar(activity))
    }

    @Test fun bothSettingsScreensExplainTheEnforcedPolicy() {
        val host = Robolectric.buildActivity(CarPlayHostActivity::class.java).get()
        val section = host.javaClass.getDeclaredMethod("buildFullscreenSection").apply { isAccessible = true }
            .invoke(host) as View
        assertTrue(texts(section).contains(host.getString(R.string.ora_fullscreen_description)))
        val settings = Robolectric.buildActivity(DiPlayActivity::class.java).get()
        val parent = LinearLayout(settings)
        settings.javaClass.getDeclaredMethod("settings", LinearLayout::class.java).apply { isAccessible = true }
            .invoke(settings, parent)
        assertTrue(texts(parent).contains(settings.getString(R.string.ora_fullscreen_description)))
        assertFalse(texts(parent).contains(settings.getString(R.string.adapt_pip_resolution)))
    }

    private fun texts(view: View): List<String> = when (view) {
        is TextView -> listOf(view.text.toString())
        is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
        else -> emptyList()
    }

    class ResettingActivity : Activity() {
        override fun onResume() {
            super.onResume()
            window.decorView.systemUiVisibility = 0
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }
}
