package com.rapzone.app.network

import android.content.Context
import android.os.Build
import com.squareup.moshi.Moshi
import com.squareup.moshi.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * App-wide configuration stored in SharedPreferences:
 * - API base URL (defaults: emulator -> 10.0.2.2, real device -> LAN IP of the dev PC)
 * - Auth token (optional, for favorites sync)
 * - Theme preference
 */
object AppConfig {
    private const val PREFS = "rapzone_config"
    private const val KEY_API_URL = "api_url"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_DARK = "dark_theme"

    /** Emulator: 10.0.2.2 aliases the host machine's localhost. */
    private const val DEFAULT_API_URL_EMULATOR = "http://10.0.2.2:4000/"

    /** Real device: reach the dev PC over the local network (update if the PC's IP changes,
     * or override it from Settings -> "رابط الخادم"). */
    private const val DEFAULT_API_URL_DEVICE = "http://192.168.1.5:4000/"

    private fun isEmulator(): Boolean {
        val model = Build.MODEL.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val product = Build.PRODUCT.lowercase()
        val fingerprint = Build.FINGERPRINT.lowercase()
        return model.contains("google") ||
            manufacturer.contains("google") ||
            product.contains("emulator") ||
            product.contains("sdk") ||
            fingerprint.contains("generic") ||
            model.contains("sdk")
    }

    private fun defaultApiUrl(): String =
        if (isEmulator()) DEFAULT_API_URL_EMULATOR else DEFAULT_API_URL_DEVICE

    fun apiUrl(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_API_URL, defaultApiUrl())
            .orEmpty()
            .let { if (it.endsWith("/")) it else "$it/" }

    fun setApiUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_API_URL, url).apply()
        ApiClient.rebuild(context)
    }

    fun token(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)

    fun setToken(context: Context, token: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().apply {
                if (token == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, token)
            }
    }

    fun isDarkTheme(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DARK, true)

    fun setDarkTheme(context: Context, dark: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DARK, dark).apply()
    }
}

/**
 * Retrofit + OkHttp + Moshi client. Rebuilt when the API URL changes.
 */
object ApiClient {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Volatile
    private var api: RapZoneApi? = null

    @Volatile
    private var currentBaseUrl: String? = null

    fun get(context: Context): RapZoneApi {
        val baseUrl = AppConfig.apiUrl(context)
        val cached = api
        if (cached != null && currentBaseUrl == baseUrl) return cached

        synchronized(this) {
            if (api != null && currentBaseUrl == baseUrl) return api!!

            val authInterceptor = okhttp3.Interceptor { chain ->
                val token = AppConfig.token(context.applicationContext)
                val request = if (token != null) {
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else chain.request()
                chain.proceed(request)
            }

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            api = retrofit.create(RapZoneApi::class.java)
            currentBaseUrl = baseUrl
            return api!!
        }
    }

    fun rebuild(context: Context) {
        synchronized(this) {
            api = null
            currentBaseUrl = null
        }
    }
}
