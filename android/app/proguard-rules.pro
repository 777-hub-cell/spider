# Preserve annotation and generic signature metadata used by Room and AndroidX.
-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses

# Room database entry points are referenced by generated implementation code.
-keep class * extends androidx.room.RoomDatabase { <init>(...); }
