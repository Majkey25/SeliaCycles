package com.majkeylab.seliacycles

import android.content.pm.ApplicationInfo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xmlpull.v1.XmlPullParser

class BackupPolicyTest {
    @Test fun compiledManifestUsesFullDeviceTransferBackup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa"))
        val info = context.applicationInfo
        assertTrue(info.flags and ApplicationInfo.FLAG_ALLOW_BACKUP != 0)
        assertTrue(info.flags and ApplicationInfo.FLAG_FULL_BACKUP_ONLY != 0)
        assertEquals(DeviceTransferBackupAgent::class.java.name, info.backupAgentName)
        context.resources.getXml(R.xml.backup_rules).use { rules ->
            var includes = 0
            while (rules.next() != XmlPullParser.END_DOCUMENT) {
                if (rules.eventType == XmlPullParser.START_TAG && rules.name == "include") {
                    assertEquals("deviceToDeviceTransfer", rules.getAttributeValue(null, "requireFlags"))
                    includes++
                }
            }
            assertEquals(2, includes)
        }
    }
}
