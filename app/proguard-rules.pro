# FR : Configuration minimale. NE PAS EFFACER.
# EN : Minimal configuration. DO NOT DELETE.

-keepattributes Signature, InnerClasses, EnclosingMethod

-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-keep class fr.croustillapp.** { *; }