package com.example.capstoneproject.network

import android.content.Context
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Single Retrofit instance for the whole app.
 *
 * BASE_URL options (dev only):
 *  - "http://127.0.0.1:8000/"  — emulator with an adb tunnel. Run once per emulator boot:
 *        adb reverse tcp:8000 tcp:8000
 *    (works even when the emulator has no network / a VPN or proxy breaks 10.0.2.2)
 *  - "http://10.0.2.2:8000/"   — emulator's alias for the host PC, when emulator networking works
 *  - "http://192.168.x.x:8000/" — a physical phone on the same Wi-Fi (PC's LAN IP); backend binds 0.0.0.0
 * Whichever host you use must also be listed in res/xml/network_security_config.xml.
 */
object ApiClient {

    private const val BASE_URL = "http://127.0.0.1:8000/"
    private const val PREFS = "AppPreferences"
    private const val KEY_TOKEN = "access_token"

    private lateinit var appContext: Context

    /** Call once from CapstoneProject.onCreate() (the Application class). */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // ---- token storage ----
    fun saveToken(token: String) =
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TOKEN, token).apply()

    fun getToken(): String? =
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)

    fun clearToken() =
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_TOKEN).apply()

    // ---- OkHttp / Retrofit ----
    private val authInterceptor = Interceptor { chain ->
        val token = getToken()
        val request = if (token != null)
            chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
        else chain.request()
        chain.proceed(request)
    }

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            // BASIC logs method, URL and status only. Never use BODY here: it prints request bodies,
            // including passwords, to Logcat.
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)   // Gemini calls can take a few seconds
            .build()
    }

    val api: WellnessApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WellnessApi::class.java)
    }

    /** Pulls a human-readable message out of a FastAPI error response. */
    fun errorMessage(response: Response<*>): String {
        val raw = response.errorBody()?.string() ?: return "HTTP ${response.code()}"
        return try {
            val detail = Gson().fromJson(raw, ApiError::class.java).detail
            when (detail) {
                is String -> detail
                is List<*> -> (detail.firstOrNull() as? Map<*, *>)?.get("msg")?.toString() ?: raw
                else -> raw
            }
        } catch (e: Exception) {
            raw
        }
    }
}
