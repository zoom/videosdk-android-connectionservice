package com.videosdkconnectionservice.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.tylerthrailkill.helpers.prettyprint.pp
import com.videosdkconnectionservice.activities.Config
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import us.zoom.sdk.ZoomVideoSDK
import us.zoom.sdk.ZoomVideoSDKAudioHelper
import us.zoom.sdk.ZoomVideoSDKAudioOption
import us.zoom.sdk.ZoomVideoSDKAudioStatus
import us.zoom.sdk.ZoomVideoSDKErrors
import us.zoom.sdk.ZoomVideoSDKInitParams
import us.zoom.sdk.ZoomVideoSDKSession
import us.zoom.sdk.ZoomVideoSDKSessionContext
import us.zoom.sdk.ZoomVideoSDKUser
import us.zoom.sdk.ZoomVideoSDKVideoOption
data class ZoomSessionUIState(
    val sessionName: String = "",
    val userName: String = "",
    val password: String? = "",
    val sessionLoader: Boolean = true,
    val muted: Boolean = false,
    val audioConnected: Boolean = false,
    val inSession: Boolean = true,
)

class ZoomSessionViewModel(application: Application): AndroidViewModel(application) {
    companion object {
        // stored reference so background components can invoke startCall for development/testing
        @SuppressLint("StaticFieldLeak")
        var instance: ZoomSessionViewModel? = null
    }
    @SuppressLint("StaticFieldLeak")
    private val context: Context = getApplication<Application>().applicationContext
    private val _zoomSessionUIState = MutableStateFlow(ZoomSessionUIState())

    //Can be used by Activity Views to access read-only private state from the frontend
    val zoomSessionUIState: StateFlow<com.videosdkconnectionservice.viewmodel.ZoomSessionUIState> = _zoomSessionUIState.asStateFlow()
    private lateinit var config: Config

    private lateinit var voipconnection: ZoomVoIPConnection

    @SuppressLint("StaticFieldLeak")
    private var connectionService: MyConnectionService? = null

    private var sdkInitialized: Boolean = false

    fun initZoomSDK (config: Config) {
        if (!sdkInitialized) {
            val initParams = ZoomVideoSDKInitParams().apply {
                domain = "https://zoom.us" //or "https://www.zoomgov.com" for ZoomGov accounts
            }
            val sdk = ZoomVideoSDK.getInstance()
            val initResult = sdk.initialize(context, initParams)
            if (initResult == ZoomVideoSDKErrors.Errors_Success) {
                println("init success")
                sdkInitialized = true
                instance = this
            } else {
                println("init fail")
                sdkInitialized = false
            }
        }
        this.config = config
        this.connectionService = MyConnectionService.getInstance()
        this.connectionService?.setParams(context, this)
        val listener = EventListener(this).listener
        ZoomVideoSDK.getInstance().addListener(listener)
    }
    fun startCall() {
        val cs = MyConnectionService.getInstance()
        cs.registerTelecomAccount()
        cs.addNewIncomingCall(config.sessionName)
    }
    fun joinSession(voipConnection: ZoomVoIPConnection) {
        //store voip connection for manual use in call
        voipconnection = voipConnection

        if (!sdkInitialized) voipconnection.disconnect()

        val config = this.config
        val joinParams: ZoomVideoSDKSessionContext = ZoomVideoSDKSessionContext().apply {
            sessionName = config.sessionName
            userName = config.userName
            sessionPassword = config.password
            token = config.jwt
            videoOption = ZoomVideoSDKVideoOption().apply { localVideoOn = false }
            audioOption = ZoomVideoSDKAudioOption().apply {
                mute = true
                connect = true
            }
        }
        val session: ZoomVideoSDKSession? = ZoomVideoSDK.getInstance().joinSession(joinParams)
        println("joining session")
        if (session != null) {
            _zoomSessionUIState.update {
                it.copy(
                    sessionName = config.sessionName,
                    userName = config.userName,
                    password = config.password
                )
            }
        }
    }
    fun getMyself(): ZoomVideoSDKUser {
        return ZoomVideoSDK.getInstance().session.mySelf
    }
    fun disconnectCall() {
        voipconnection.disconnect()
    }
    fun closeSession(end: Boolean) {
        _zoomSessionUIState.update {
            it.copy(
                sessionName = "",
                userName = "",
                password = "",
                sessionLoader = true,
                muted = false,
                audioConnected = false,
                inSession = false,
            )
        }
        ZoomVideoSDK.getInstance().leaveSession(end)
    }
    fun updateState(state: ZoomSessionUIState) {
        _zoomSessionUIState.value = state
    }
    fun getState(): ZoomSessionUIState {
        return _zoomSessionUIState.value
    }
    fun toggleMicrophone() {
        val user: ZoomVideoSDKUser = ZoomVideoSDK.getInstance().session.mySelf
        val audioHelper: ZoomVideoSDKAudioHelper = ZoomVideoSDK.getInstance().audioHelper
        val audioType: ZoomVideoSDKAudioStatus.ZoomVideoSDKAudioType? = user.audioStatus?.audioType

        if (audioType == ZoomVideoSDKAudioStatus.ZoomVideoSDKAudioType.ZoomVideoSDKAudioType_None) {
            println("Starting Audio...")
            audioHelper.startAudio()
            _zoomSessionUIState.update { it.copy( audioConnected = true, muted = true )}
        } else {
            val muted: Boolean? = user.audioStatus?.isMuted
            if (muted != null) {
                if (muted) {
                    audioHelper.unMuteAudio(user)
                } else {
                    audioHelper.muteAudio(user)
                }
            }
        }
    }
}