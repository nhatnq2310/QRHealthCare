package com.qrhealthcare.app.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Single Retrofit + OkHttp client for the whole app.
 *
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  ONE THING TO CHANGE: BASE_URL                                           ║
 * ║                                                                          ║
 * ║  - Android emulator → http://10.0.2.2:4000/api/v1/                      ║
 * ║    (10.0.2.2 is the emulator's alias for the host machine's loopback.)  ║
 * ║                                                                          ║
 * ║  - Physical phone on same WiFi → http://<YOUR-LAN-IP>:4000/api/v1/      ║
 * ║    Find your computer's LAN IP:                                          ║
 * ║      macOS:   `ipconfig getifaddr en0`                                   ║
 * ║      Linux:   `hostname -I`                                              ║
 * ║      Windows: `ipconfig` → IPv4 Address under your WiFi adapter         ║
 * ║    Phone and computer must be on the same network.                       ║
 * ║                                                                          ║
 * ║  - Deployed backend → https://your-domain.com/api/v1/                   ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
object ApiClient {

    private const val BASE_URL = "http://10.0.2.2:4000/api/v1/"

    /**
     * Base URL embedded in QR codes that get printed on stickers / shown in-app.
     * MUST be publicly reachable for the QR-scan-with-phone-camera flow to work
     * (a stranger's phone is not on your LAN).
     *
     * During development you can leave it pointing at your LAN backend; only
     * QRs you scan yourself on the same network will resolve. For production,
     * change to your deployed domain, e.g. "https://qrhealthcare.vn/p/".
     *
     * Derived from BASE_URL by default — strips "/api/v1/" and appends "/p/".
     */
    val PUBLIC_PROFILE_URL_BASE: String =
        BASE_URL.removeSuffix("/").removeSuffix("/api/v1") + "/p/"

    /** Convenience: full URL for a given tag code, e.g. "http://.../p/QRH-A1B2". */
    fun publicProfileUrl(tagCode: String): String = PUBLIC_PROFILE_URL_BASE + tagCode

    /** Convenience: absolute URL for a relative upload path like "/uploads/abc.jpg". */
    fun uploadUrl(relativePath: String): String {
        if (relativePath.startsWith("http")) return relativePath
        val host = BASE_URL.removeSuffix("/").removeSuffix("/api/v1")
        return host + (if (relativePath.startsWith("/")) relativePath else "/$relativePath")
    }

    /**
     * Live JWT — set by AppRepository after a successful login/register and
     * cleared on logout. Read by the auth interceptor below on every request.
     *
     * @Volatile because the interceptor runs on OkHttp's dispatcher thread
     * while writes happen on the main / IO scope.
     */
    @Volatile
    var authToken: String? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Logs full request/response bodies in Logcat — flip to NONE for release builds.
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = okhttp3.Interceptor { chain ->
        val original = chain.request()
        // Don't decorate the auth endpoints themselves (they're public).
        val isAuthRoute = original.url.encodedPath.contains("/auth/")
        val token = authToken
        val request = if (token != null && !isAuthRoute) {
            original.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else original
        chain.proceed(request)
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}
