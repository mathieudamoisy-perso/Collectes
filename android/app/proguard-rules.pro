# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# OkHttp / Kotlin
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Jsoup
-keep class org.jsoup.** { *; }

# PDFBox Android
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.**
-dontwarn org.apache.**

# WorkManager
-keep class androidx.work.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Receivers / workers referenced from the manifest
-keep class com.smirtom.app.notifications.** { *; }
