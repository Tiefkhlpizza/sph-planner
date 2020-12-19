package de.koenidv.sph.networking

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.OkHttpResponseListener
import com.androidnetworking.interfaces.StringRequestListener
import com.facebook.stetho.okhttp3.StethoInterceptor
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.SphPlanner.Companion.applicationContext
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.parsing.RawParser
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.*
import java.util.concurrent.TimeUnit

//  Created by koenidv on 11.12.2020.
@Suppress("PropertyName")
class NetworkManager {

    val FAILED_UNKNOWN = -1
    val SUCCESS = 0
    val FAILED_NO_NETWORK = 1
    val FAILED_INVALID_CREDENTIALS = 2
    val FAILED_MAINTENANCE = 3
    val FAILED_CANCELLED = 4

    // todo save last refresh for checks
    // todo use lambdas
    // todo handle errors
    fun createCourseIndex(onComplete: (success: Int) -> Unit) {
        val prefs = applicationContext().getSharedPreferences("sharedPrefs", AppCompatActivity.MODE_PRIVATE)
        // Remove old courses, it'll just lead to isses
        val coursesDb = CoursesDb.getInstance()
        coursesDb.clear()
        // Set courses last updated to 0 in case this gets cancelled
        prefs.edit().putLong("courses_last_updated", 0).apply()


        // Firstly, load courses from timetable so we have an overview
        loadSiteWithToken("https://start.schulportal.hessen.de/stundenplan.php", object : StringRequestListener {
            override fun onResponse(response: String?) {
                coursesDb.save(RawParser().parseCoursesFromTimetable(response!!))

                // Secondly, load those courses from study groups to find out where the user belongs
                loadSiteWithToken("https://start.schulportal.hessen.de/lerngruppen.php", object : StringRequestListener {
                    override fun onResponse(response: String?) {
                        coursesDb.setNulledNotFavorite()
                        coursesDb.save(RawParser().parseCoursesFromStudygroups(response!!))

                        // Lastly, load courses again from posts overview to get number ids
                        loadSiteWithToken("https://start.schulportal.hessen.de/meinunterricht.php", object : StringRequestListener {
                            override fun onResponse(response: String?) {
                                coursesDb.save(RawParser().parseCoursesFromPostsoverview(response!!))
                                onComplete(SUCCESS)
                                // Remember when we last updated the courses
                                prefs.edit().putLong("courses_last_updated", Date().time).apply()
                            }

                            override fun onError(anError: ANError?) {
                                onComplete(FAILED_UNKNOWN)
                            }
                        })
                    }

                    override fun onError(anError: ANError?) {
                        onComplete(FAILED_UNKNOWN)

                    }
                })

            }

            override fun onError(anError: ANError?) {
                onComplete(FAILED_UNKNOWN)
            }
        })
    }


    /**
     * Loads a url within the sph and handles authentication
     * @param url URL to load
     * @param listener Listen for results
     * todo check if request was successfull (signed in)
     */
    fun loadSiteWithToken(url: String, listener: StringRequestListener) {

        // Getting an access token
        TokenManager().generateAccessToken(object : TokenManager.TokenGeneratedListener {
            override fun onTokenGenerated(success: Int, token: String) {
                // Setting sid cookie
                CookieStore.saveFromResponse(
                        HttpUrl.parse("https://schulportal.hessen.de")!!,
                        listOf(Cookie.Builder().domain("schulportal.hessen.de").name("sid").value(token).build()))

                // Adding an Network Interceptor for Debugging purpose :
                val okHttpClient = OkHttpClient.Builder()
                        .addNetworkInterceptor(StethoInterceptor())
                        .cookieJar(CookieStore)
                        .connectTimeout(60, TimeUnit.SECONDS) // sph timeout is 30 seconds
                        .build()
                AndroidNetworking.initialize(applicationContext(), okHttpClient)

                // Getting webpage
                AndroidNetworking.get(url)
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.27 Safari/537.36")
                        .setPriority(Priority.LOW)
                        .build()
                        .getAsString(listener)
            }
        })

    }

    fun resolveUrl(url: String, onComplete: (success: Int, resolvedUrl: String) -> Unit) {
        // Getting an access token
        TokenManager().generateAccessToken(object : TokenManager.TokenGeneratedListener {
            override fun onTokenGenerated(success: Int, token: String) {
                // Setting sid cookie
                CookieStore.saveFromResponse(
                        HttpUrl.parse("https://schulportal.hessen.de")!!,
                        listOf(Cookie.Builder().domain("schulportal.hessen.de").name("sid").value(token).build()))

                // Adding an Network Interceptor for Debugging purpose :
                val okHttpClient = OkHttpClient.Builder()
                        .addNetworkInterceptor(StethoInterceptor())
                        .cookieJar(CookieStore)
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .build()
                AndroidNetworking.initialize(applicationContext(), okHttpClient)

                // Getting webpage as OkHttp
                AndroidNetworking.get(url)
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.27 Safari/537.36")
                        .setPriority(Priority.LOW)
                        .build()
                        .getAsOkHttpResponse(object : OkHttpResponseListener {
                            override fun onResponse(response: Response) {
                                // In some cases, sph redirects back to the home page (why?)
                                // In this case, ignore the new response
                                if (response.request().url().toString() != "https://start.schulportal.hessen.de/index.php") {
                                    onComplete(SUCCESS, response.request().url().toString())
                                } else {
                                    onComplete(FAILED_UNKNOWN, url)
                                }
                            }

                            override fun onError(anError: ANError?) {
                                // Running in emulator will cause an ssl errora
                                // Network error will also happen if there is a timeout or no connection
                                // Not a huge deal though, the unresolved url will work as well. For now.
                                Log.e(SphPlanner.TAG, anError!!.errorDetail + ": " + url)
                                onComplete(FAILED_UNKNOWN, url)
                            }

                        })
            }
        })
    }
}