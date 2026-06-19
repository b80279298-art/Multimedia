# Regras ProGuard para DualStreamPlayer Pro

# Manter classes do Media3/ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Manter classes do Material Design
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Manter enums do projeto
-keepclassmembers enum com.dualstreamplayer.pro.** { *; }

# Manter data classes Kotlin (evitar problemas de serialização)
-keep class com.dualstreamplayer.pro.PlayerSettings { *; }
-keep class com.dualstreamplayer.pro.PlayerHolder { *; }
-keep class com.dualstreamplayer.pro.AudioFocusLevel { *; }
-keep class com.dualstreamplayer.pro.LayoutMode { *; }

# Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
