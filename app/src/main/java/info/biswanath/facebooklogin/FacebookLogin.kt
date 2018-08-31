package info.biswanath.facebooklogin

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class FacebookLogin {
    private var callbackManager: CallbackManager? = null
    private var accessTokenTracker: AccessTokenTracker? = null
    private var profileTracker: ProfileTracker? = null
    private var activity: AppCompatActivity

    constructor(activity: AppCompatActivity) {
        this.activity = activity
        configureFbLogin()
    }

    private fun configureFbLogin() {
        callbackManager = CallbackManager.Factory.create()
        LoginManager.getInstance().logInWithReadPermissions(activity, Arrays.asList("public_profile", "email"))
        LoginManager.getInstance().registerCallback(callbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(loginResult: LoginResult) {
                        // App code
                        //prepare a GraphRequest object
                        val request = GraphRequest.newMeRequest(loginResult.accessToken)
                        { `object`: JSONObject?,
                          response ->
                            if (response.error != null)
                                activity.toast(response?.error?.errorMessage?.toString()
                                        ?: "Facebook sent an Error")
                            else
                                extractDataFromFbResponse(`object`, loginResult)
                        }

                        //prepare permission list to send via GraphRequest object
                        val parameters = Bundle()
                        parameters.putString("fields", "id, first_name, last_name, email")

                        //add permission and invoke
                        request.parameters = parameters
                        request.executeAsync()
                    }

                    override fun onCancel() {
                        // App code
                        activity.toast("Login cancelled.")
                    }

                    override fun onError(exception: FacebookException) {
                        // App code
                        activity.toast("Login error: ${exception.message}")
                    }
                })
        /*accessTokenTracker = object : AccessTokenTracker() {
            override fun onCurrentAccessTokenChanged(oldAccessToken: AccessToken?, currentAccessToken: AccessToken?) {
                configureAppLoginBtnText()
            }
        }
        profileTracker = object : ProfileTracker() {
            override fun onCurrentProfileChanged(oldProfile: Profile?, currentProfile: Profile?) {
                configureAppLoginBtnText()
            }
        }
        accessTokenTracker?.startTracking()
        profileTracker?.startTracking()*/
    }

    private fun extractDataFromFbResponse(`object`: JSONObject?, loginResult: LoginResult) {
        try {
            var acc_id = ""
            `object`?.let {
                if (`object`.has("id"))
                    acc_id = `object`.optString("id", "0")

                val firstName = `object`.optString("first_name", "")
                val lastName = `object`.optString("last_name", "")
                val email = `object`.optString("email", "")

                var profilePic = ""
                if (acc_id != "0")
                    profilePic = "https://graph.facebook.com/$acc_id/picture?type=large"

                val regToken = loginResult.accessToken.token

                (activity as FacebookDataListener).onDataReceived(FacebookModel(
                        acc_id, firstName, lastName, email, profilePic, regToken
                ))

                LoginManager.getInstance().logOut()
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        callbackManager?.onActivityResult(requestCode, resultCode, data)
    }

    interface FacebookDataListener {
        fun onDataReceived(fbData: FacebookModel)
    }


}

data class FacebookModel(val fbId: String,
                         val fName: String,
                         val lName: String,
                         val email: String,
                         val profilePic: String,
                         val regToken: String)

private fun AppCompatActivity.toast(message: String, showLongToast: Boolean = false) {
    if (showLongToast)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    else
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

