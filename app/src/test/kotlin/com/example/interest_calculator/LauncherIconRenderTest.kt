package com.example.interest_calculator

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the launcher icon from the real resources (adaptive layers under circle and squircle
 * masks, plus the themed monochrome layer) to PNGs under build/icon, and exports the 512 px
 * Play Store icon.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class LauncherIconRenderTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val out = File("build/icon").apply { mkdirs() }

    private fun save(bitmap: Bitmap, name: String) {
        File(out, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Draws an adaptive icon layer the way launchers do: 108 units, of which the middle 72 show. */
    private fun Canvas.drawLayer(layer: Drawable, size: Int) {
        val full = size * 108 / 72
        val inset = (full - size) / 2
        layer.setBounds(-inset, -inset, size + inset, size + inset)
        layer.draw(this)
    }

    private fun masked(icon: AdaptiveIconDrawable, size: Int, mask: Path): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            clipPath(mask)
            drawLayer(icon.background, size)
            drawLayer(icon.foreground, size)
        }
        return bitmap
    }

    private fun squircle(size: Float) = Path().apply {
        // Superellipse-like rounded square used by many launchers (Pixel / One UI style).
        addRoundRect(RectF(0f, 0f, size, size), size * 0.30f, size * 0.30f, Path.Direction.CW)
    }

    private fun circle(size: Float) = Path().apply { addCircle(size / 2, size / 2, size / 2, Path.Direction.CW) }

    @Test
    fun renderLauncherIcons() {
        val icon = context.getDrawable(R.mipmap.ic_launcher) as AdaptiveIconDrawable
        assertTrue(icon.monochrome != null)

        val size = 432
        save(masked(icon, size, circle(size.toFloat())), "adaptive_circle.png")
        save(masked(icon, size, squircle(size.toFloat())), "adaptive_squircle.png")

        // Android 13+ themed icon: tinted monochrome layer on a tonal background.
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bmp ->
            Canvas(bmp).apply {
                clipPath(circle(size.toFloat()))
                drawColor(Color.parseColor("#FFE9DDFF"))
                val mono = icon.monochrome!!.mutate()
                mono.colorFilter = PorterDuffColorFilter(Color.parseColor("#FF3A1C9E"), PorterDuff.Mode.SRC_IN)
                drawLayer(mono, size)
            }
            save(bmp, "themed_monochrome.png")
        }

        // Google Play store listing icon: 512 x 512, full-bleed square (Play applies its own mask).
        Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).also { bmp ->
            Canvas(bmp).apply {
                drawLayer(icon.background, 512)
                drawLayer(icon.foreground, 512)
            }
            save(bmp, "ic_launcher-playstore.png")
        }

        // Contact sheet: icons at launcher size on light and dark wallpapers.
        val sheet = Bitmap.createBitmap(1200, 560, Bitmap.Config.ARGB_8888)
        Canvas(sheet).apply {
            drawColor(Color.parseColor("#FFF3EDF7"))
            val dark = Paint().apply { color = Color.parseColor("#FF1D1B20") }
            drawRect(0f, 280f, 1200f, 560f, dark)
            listOf("adaptive_circle.png", "adaptive_squircle.png", "themed_monochrome.png").forEachIndexed { i, name ->
                val bmp = android.graphics.BitmapFactory.decodeFile(File(out, name).path)
                val small = Bitmap.createScaledBitmap(bmp, 192, 192, true)
                drawBitmap(small, 80f + i * 380f, 44f, null)
                drawBitmap(small, 80f + i * 380f, 324f, null)
            }
        }
        save(sheet, "contact_sheet.png")
    }
}
