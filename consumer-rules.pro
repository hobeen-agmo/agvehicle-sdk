# AAR을 넣은 앱의 R8/ProGuard가 AIDL 스텁·콜백을 제거하지 않도록 보존.
# (Binder 스텁은 리플렉션/네이티브 경계라 축소기가 오판할 수 있다)
-keep class farm.agmo.vehicle.** { *; }
