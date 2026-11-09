package com.max.assistant.protection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryProtectionTest {

    @Test
    fun testGalleryPackagesDetected() {
        assertTrue(GalleryProtectionManager.isGalleryPackage("com.sec.android.gallery3d"))
        assertTrue(GalleryProtectionManager.isGalleryPackage("com.google.android.apps.photos"))
        assertTrue(GalleryProtectionManager.isGalleryPackage("com.android.gallery3d"))
        assertFalse(GalleryProtectionManager.isGalleryPackage("com.google.android.youtube"))
        assertFalse(GalleryProtectionManager.isGalleryPackage("com.android.chrome"))
    }

    @Test
    fun testVoiceCommandsBlocked() {
        GalleryProtectionManager.setProtectionActive(true)
        // English
        assertTrue(GalleryProtectionManager.shouldBlockVoiceLaunch("open gallery"))
        assertTrue(GalleryProtectionManager.shouldBlockVoiceLaunch("show my photos"))
        assertTrue(GalleryProtectionManager.shouldBlockVoiceLaunch("launch pictures"))
        // Hindi
        assertTrue(GalleryProtectionManager.shouldBlockVoiceLaunch("gallery kholo"))
        assertTrue(GalleryProtectionManager.shouldBlockVoiceLaunch("photo dikhao"))
        assertTrue(GalleryProtectionManager.shouldBlockVoiceLaunch("tasveer dikhao"))
        assertTrue(GalleryProtectionManager.shouldBlockVoiceLaunch("गैलरी खोलो"))
        assertTrue(GalleryProtectionManager.shouldBlockVoiceLaunch("मेरी फोटो दिखाओ"))
        // Bengali
        assertTrue(GalleryProtectionManager.shouldBlockVoiceLaunch("গ্যালারি খোলো"))
        assertTrue(GalleryProtectionManager.shouldBlockVoiceLaunch("আমার ছবি দেখাও"))
        assertTrue(GalleryProtectionManager.shouldBlockVoiceLaunch("chobi dekhaw"))
        // Allowed
        assertFalse(GalleryProtectionManager.shouldBlockVoiceLaunch("open youtube"))
        assertFalse(GalleryProtectionManager.shouldBlockVoiceLaunch("youtube chalao"))
        assertFalse(GalleryProtectionManager.shouldBlockVoiceLaunch("camera kholo"))
    }
}
