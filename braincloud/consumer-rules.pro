# Keep the braincloud.cfg reader; init(Context) loads it by reflection.
-keep class com.bitheads.braincloud.support.NativeConfig { public *; }
-keep interface com.bitheads.braincloud.support.NativeConfig$Callback { *; }
