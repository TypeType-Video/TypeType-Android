package dev.typetype.android.data.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import dev.typetype.android.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 24)
class NetworkSecurityConfigAndroidTest {
    @Test
    fun applicationTrustsSystemAndUserCertificateAuthorities() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = context.resources.getXml(R.xml.network_security_config)
        val certificateSources = mutableSetOf<String>()
        var cleartextPermitted: String? = null
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "base-config" -> cleartextPermitted = parser.getAttributeValue(null, "cleartextTrafficPermitted")
                    "certificates" -> parser.getAttributeValue(null, "src")?.let(certificateSources::add)
                }
            }
            parser.next()
        }

        assertEquals("true", cleartextPermitted)
        assertEquals(setOf("system", "user"), certificateSources)
    }
}
