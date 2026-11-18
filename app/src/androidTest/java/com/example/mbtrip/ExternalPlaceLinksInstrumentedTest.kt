package com.example.mbtrip

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mbtrip.recommendation.*
import com.example.mbtrip.timeline.model.Member
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** Opt-in check that clicks the production buttons and opens a real installed browser/map app. */
class ExternalPlaceLinksInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    @Test fun selectedPlaceOpensExternalApp() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("mbtripExternalLinks") == "true")
        val target = requireNotNull(args.getString("mbtripLinkTarget"))
        val place = SiheungPlaces.all.first()
        val (label, url) = when (target) {
            "google" -> "구글지도" to PlaceLinks.googleMaps(place)
            "naver" -> "네이버지도" to PlaceLinks.naverMap(place)
            "blog" -> "블로그 후기" to PlaceLinks.naverBlog(place)
            else -> error("Unknown target")
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        compose.setContent { MaterialTheme {
            RecommendationDialog(emptyList(), listOf(Member("test", "테스터", "@test", 50)),
                "link-check", LocalDate.now(), {}, {})
        } }
        compose.onAllNodesWithText(label).onFirst().performScrollTo().performClick()
        var state = ""
        for (attempt in 0..19) {
            Thread.sleep(500)
            state = android.os.ParcelFileDescriptor.AutoCloseInputStream(
                instrumentation.uiAutomation.executeShellCommand("dumpsys activity activities")).bufferedReader().use { it.readText() }
            val resumed = state.lineSequence().filter { it.contains("ResumedActivity") }.joinToString("\n")
            val windowPackage = instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString().orEmpty()
            if ((resumed.contains("com.android.chrome") || resumed.contains("com.google.android.apps.maps")) &&
                windowPackage in setOf("com.android.chrome", "com.google.android.apps.maps")) break
        }
        val resumed = state.lineSequence().filter { it.contains("ResumedActivity") }.joinToString("\n")
        val dir = instrumentation.targetContext.getExternalFilesDir(null)
        File(dir, "link-$target.json").writeText(JSONObject().put("target",target).put("expectedUrl",url).put("resumedActivity",resumed).toString())
        File(dir, "link-$target-activities.txt").writeText(state)
        Thread.sleep(1500)
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(dir, "link-$target.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            bitmap.recycle()
        }
        assertTrue("No browser/map activity opened: $resumed", resumed.contains("com.android.chrome") || resumed.contains("com.google.android.apps.maps"))
    }
}
