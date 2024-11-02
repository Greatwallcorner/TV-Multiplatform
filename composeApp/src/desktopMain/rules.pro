-printmapping build/release-mapping.txt
-libraryjars  <java.home>/jmods/java.base.jmod(!**.jar;!module-info.class)

-keep class org.jboss.marshalling.** {*;}
#-keep class org.conscrypt.** {*;}
-keep class org.sqlite.** {*;}
#-keep class org.bouncycastle.** {*;}
-keep class ch.qos.** {*;}
-keep class org.eclipse.jetty.** {*;}
-keep class org.eclipse.jetty.** {*;}
#-keep class reactor.blockhound.** {*;}
#-keep class com.oracle.svm.** {*;}
#-keep class com.sun.activation.** {*;}
#-keep class org.graalvm.nativeimage.** {*;}
-keep class com.sun.jna.** {*;}
-keep class javax.swing.** {*;}

#tls 1.3
#-keep class org.osgi.** {*;}
-keep class com.google.appengine.** {*;}
-keep class com.google.apphosting.** {*;}
-keep class com.google.zxing.** {*;}
-keep class org.apache.** {*;}
#ssl
-keep class org.openjsse.** {*;}
-keep class io.netty.** {*;}
# ssh
#-keep class com.jcraft.** {*;}
#压缩
#-keep class com.aayushatharva.** {*;}
#-keep class com.github.luben.** {*;}
#-keep class com.ning.compress.** {*;}
# lz4压缩
#-keep class net.jpountz.** {*;}
#jackson
-keep class org.codehaus.** {*;}
-keep class com.github.sardine.** {*;}
-keep class io.ktor.** {*;}
#-keep class com.corner.**{*;}
-keep class org.jupnp.** {*;}
-keep class uk.co.caprica.vlcj.** {*;}
# vlcj 的一些其他类
-keep class uk.co.caprica.vlcj.player.** { *; }
-keep class uk.co.caprica.vlcj.factory.** { *; }
-keep class uk.co.caprica.vlcj.log.** { *; }

# 保留 vlcj 的接口和回调
-keep interface uk.co.caprica.vlcj.** { *; }
-keep class * implements uk.co.caprica.vlcj.** { *; }

# 保留 vlcj 的静态方法
-keep,allowobfuscation class uk.co.caprica.vlcj.** {
    public static void main(java.lang.String[]);
    public static ** valueOf(java.lang.String);
    public static ** valueOf(int);
    public static ** of(...);
    public static ** create(...);
}
-keep class org.slf4j.** {*;}
-keep class kotlinx.serialization.** {*;}
# disable optimisation for descriptor field because in some versions of ProGuard, optimization generates incorrect bytecode that causes a verification error
# see https://github.com/Kotlin/kotlinx.serialization/issues/2719
-keepclassmembers public class **$$serializer {
    private ** descriptor;
}
-keep class org.json.** {*;}
-keep class com.google.gson.** {*;}
-keep class okhttp3.** {*;}
-keep class org.koin.** {*;}
-keep class kotlinx.coroutines.** {*;}
-keep class com.github.catvod.** {*;}
-keep class okio.** {*;}
-keep class com.seiko.imageloader.** {*;}
-keep class kotlinx.html.** {*;}
-keep class com.google.common.** {*;}
-keep class org.jsoup.** {*;}
-keep class app.cash.sqldelight.** {*;}
-keep class com.arkivanov.decompose.** {*;}
-keep class MainKt {*;}
-keep class com.corner.init.** {*;}
-keep class com.corner.server.** {*;}
-keep class cn.hutool.** {*;}
# -keep class androidx.compose.** {*;}
-keep class androidx.compose.foundation.** {*;}
-keep class androidx.compose.material3.** {*;}
-keep class androidx.compose.ui.** {*;}
-keep class androidx.compose.runtime.** {*;}



-dontwarn org.jboss.marshalling.**
-dontwarn org.conscrypt.**
-dontwarn org.eclipse.jetty.**
-dontwarn io.netty.internal.**
-dontwarn reactor.blockhound.**
-dontwarn org.apache.logging.**
-dontwarn com.oracle.svm.**
-dontwarn com.sun.activation.**
-dontwarn org.graalvm.nativeimage.**
-dontwarn org.bouncycastle.**
-dontwarn org.osgi.**
-dontwarn com.google.appengine.**
-dontwarn com.google.apphosting.**
-dontwarn org.apache.**
-dontwarn okhttp3.internal.platform.android.**
-dontwarn io.netty.**
-dontwarn ch.qos.**
-dontwarn com.github.sardine.**
-dontwarn javax.servlet.**
-dontwarn javax.mail.**
-dontwarn org.apache.tools.ant.**
-dontwarn okhttp3.internal.platform.Android10Platform.**
-dontwarn okhttp3.internal.platform.AndroidPlatform.**
-dontwarn android.**
-dontwarn org.openjsse.**
-dontwarn org.jupnp.**
-dontwarn cn.hutool.**
-dontusemixedcaseclassnames
-verbose