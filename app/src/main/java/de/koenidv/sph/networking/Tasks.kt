package de.koenidv.sph.networking

import android.app.Activity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.StringRequestListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.TasksDb
import de.koenidv.sph.objects.Task

//  Created by koenidv on 31.01.2021.
class Tasks {

    /**
     * Get a lambda to handle task checked changes in posts
     * Will mark task as done in db and sph and show an error if that failed
     */
    fun onCheckedChanged(activity: Activity, courseNumberId: String? = null, callback: ((Task, Boolean) -> Unit)? = null):
            (task: Task, isDone: Boolean) -> Unit = { task, isDone ->
        val numberId = courseNumberId ?: CoursesDb.getInstance().getNumberId(task.id_course)
        complete(numberId, task, isDone) {
            if (it == NetworkManager.SUCCESS) {
                if (callback != null) callback(task, isDone)
            } else {
                Snackbar.make(activity.findViewById(R.id.nav_host_fragment),
                        SphPlanner.applicationContext().getString(R.string.task_not_synchronized)
                                + " ($it)", Snackbar.LENGTH_SHORT)
                        .setAnchorView(R.id.nav_view).show()
            }
        }
    }

    /**
     * Mark a task as done in the db and send a post to sph to mark it as read there, too
     * @param numberId NumberId of the course the task belongs to
     * @param task Task that should be marked as done
     * @param isDone Whether the task is now done or not
     */
    // todo retry later on error
    private fun complete(numberId: String, task: Task, isDone: Boolean, callback: (success: Int) -> Unit) {
        // Mark as (un)done in the db
        TasksDb.getInstance().setDone(task.taskId, isDone)
        // Mark as done on sph
        // Cancel potential pending requests for this same task, just to be sure
        AndroidNetworking.cancel(task.taskId)
        // We need an access token first
        TokenManager().generateAccessToken { success: Int, token: String? ->
            if (success == NetworkManager.SUCCESS) {

                if (Firebase.remoteConfig.getBoolean("token_fix_0130")) {
                    CookieStore.clearCookies()
                } else {
                    // Make sure session id cookie is set
                    CookieStore.setToken(token!!)
                }

                // Send a post request to let sph know the task is done
                val request = AndroidNetworking.post(
                        SphPlanner.applicationContext().getString(R.string.url_mycourses)
                )
                        .addBodyParameter("a", "sus_homeworkDone")
                        .addBodyParameter("id", numberId)
                        .addBodyParameter("entry", task.id_post.substring(task.id_post.lastIndexOf("_") + 1))
                        .addBodyParameter("b", if (isDone) "done" else "undone")
                        .setTag(task.taskId)

                if (Firebase.remoteConfig.getBoolean("token_fix_0130")) {
                    request.addHeaders("Cookie", "sid=$token")
                }

                request.build()
                        .getAsString(object : StringRequestListener {
                            override fun onResponse(response: String) {
                                if (response == "1")
                                    callback(NetworkManager.SUCCESS)
                                else
                                    callback(NetworkManager.FAILED_UNKNOWN)
                            }

                            override fun onError(error: ANError) {
                                when (error.errorDetail) {
                                    "connectionError" -> callback(NetworkManager.FAILED_NO_NETWORK)
                                    "requestCancelledError" -> callback(NetworkManager.FAILED_CANCELLED)
                                    else -> callback(NetworkManager.FAILED_UNKNOWN)
                                }
                            }
                        })

            } else callback(success)
        }
    }

}