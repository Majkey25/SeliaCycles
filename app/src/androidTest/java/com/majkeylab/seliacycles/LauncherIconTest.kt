package com.majkeylab.seliacycles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.xmlpull.v1.XmlPullParser

class LauncherIconTest {
    @Test
    fun renderSystemMaskedIconForVisualReview() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa"))
        val icon = context.applicationInfo.loadIcon(context.packageManager)
        val parser = context.resources.getXml(R.mipmap.ic_launcher)
        try {
            while (parser.eventType != XmlPullParser.START_TAG && parser.eventType != XmlPullParser.END_DOCUMENT) parser.next()
            assertEquals("adaptive-icon", parser.name)
        } finally {
            parser.close()
        }
        File(context.getExternalFilesDir(null), "code21-icon-info.txt").writeText(icon.javaClass.name)
        for (size in listOf(48, 192)) {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            try {
                icon.setBounds(0, 0, size, size)
                icon.draw(Canvas(bitmap))
                assertTrue(bitmap.getPixel(size / 2, size / 2) ushr 24 > 0)
                File(context.getExternalFilesDir(null), "code21-icon-$size.png").outputStream().use {
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                }
                var headerTop = size
                for (y in 0 until size) for (x in 0 until size) {
                    val pixel = bitmap.getPixel(x, y)
                    if (Color.alpha(pixel) > 240 && Color.red(pixel) < 140 &&
                        Color.green(pixel) < 120 && Color.blue(pixel) < 160) headerTop = minOf(headerTop, y)
                }
                assertTrue("Calendar header missing at $size px", headerTop < size)
                assertTrue("Calendar tabs touch the launcher edge at $size px: $headerTop", headerTop >= size / 16)
            } finally {
                bitmap.recycle()
            }
        }
    }
}
