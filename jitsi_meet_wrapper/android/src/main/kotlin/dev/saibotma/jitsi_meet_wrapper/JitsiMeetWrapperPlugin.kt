package dev.saibotma.jitsi_meet_wrapper

import android.app.Activity
import android.content.Intent
import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.jitsi.meet.sdk.JitsiMeetUserInfo
import java.net.URL

const val JITSI_PLUGIN_TAG = "JITSI_MEET_PLUGIN"
const val JITSI_METHOD_CHANNEL = "jitsi_meet"
const val JITSI_MEETING_CLOSE = "JITSI_MEETING_CLOSE"

class JitsiMeetWrapperPlugin(activity: Activity) : FlutterPlugin, MethodCallHandler, ActivityAware {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel

    private var activity: Activity? = activity

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, JITSI_METHOD_CHANNEL)
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "joinMeeting" -> joinMeeting(call, result)
            "closeMeeting" -> closeMeeting(call, result)
            else -> result.notImplemented()
        }
    }

    private fun joinMeeting(call: MethodCall, result: Result) {
        val room = call.argument<String>("room")!!
        if (room.isBlank()) {
            result.error(
                    "400",
                    "room can not be null or empty",
                    "room can not be null or empty"
            )
            return
        }

        val serverUrlString = call.argument<String>("serverUrl") ?: "https://meet.jit.si"
        val serverUrl = URL(serverUrlString)

        val subject: String? = call.argument("subject")
        val token: String? = call.argument("token")
        val isAudioMuted: Boolean = call.argument("isAudioMuted")!!
        val isAudioOnly: Boolean = call.argument("isAudioOnly")!!
        val isVideoMuted: Boolean = call.argument("isVideoMuted")!!

        val userInfo = JitsiMeetUserInfo().apply {
            displayName = call.argument("userDisplayName")
            email = call.argument("userEmail")
            val userAvatarUrlString: String? = call.argument("userAvatarUrl")
            avatar = if (userAvatarUrlString != null) URL(userAvatarUrlString) else null
        }

        val options = JitsiMeetConferenceOptions.Builder().run {
            setRoom(room)
            setServerURL(serverUrl)
            setSubject(subject)
            setToken(token)
            setAudioMuted(isAudioMuted)
            setAudioOnly(isAudioOnly)
            setVideoMuted(isVideoMuted)
            setUserInfo(userInfo)

            val featureFlags = call.argument<HashMap<String, Any>?>("featureFlags")!!
            featureFlags.forEach { (key, value) ->
                // TODO(saibotma): Streamline with iOS implementation.
                if (value is Boolean) {
                    val boolValue = value.toString().toBoolean()
                    setFeatureFlag(key, boolValue)
                } else {
                    val intValue = value.toString().toInt()
                    setFeatureFlag(key, intValue)
                }
            }

            build()
        }


        JitsiMeetWrapperPluginActivity.launchActivity(activity, options)
        result.success("Successfully joined room: $room")
    }

    private fun closeMeeting(call: MethodCall, result: Result) {
        val intent = Intent(JITSI_MEETING_CLOSE)
        activity?.sendBroadcast(intent)
        result.success(null)
    }

    override fun onDetachedFromActivity() {
        this.activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        this.activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}