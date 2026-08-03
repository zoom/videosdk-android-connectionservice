# Video SDK Android - ConnectionService API Integration

This is a Sample App of the Android Zoom Video SDK built with Kotlin. This app showcases an example of using the Connection Service API to join and start Zoom sessions using Androids Native Call UI.

Use of this Sample App is subject to our [Terms of Use](https://www.zoom.com/en/trust/terms/).

## Prerequisites

### System Requirements
- **JDK 17** (required by build.gradle.kts)
- **Android SDK:**
  - minSdk: API 35 (Android 15)
  - targetSdk: API 37 (Android 15)
  - compileSdk: API 37
- **Android Studio** (latest stable)
- **Gradle** (included via Gradle wrapper)
- **ADB CLI Tool**

### Zoom Video SDK
- Zoom Video SDK Core dependency
- Video SDK Key and Secret from Zoom

### Device Requirements
- Android device or emulator running Android 15+ (API 35+)
- Microphone access (runtime permission requested)

## Installation

Clone this repo into your local environment:
```
$ git clone https://github.com/zoom/videosdk-android-connectionservice.git 
```

Once cloned, navigate to the 'videosdk-connectionservice' directory

You can use the `studio .` command to open it in Android Studio.

In the `env-sample` file found in `app/src/main/assets`, you can choose to either enter an endpoint url to a backend server which handles JWT Token generation or you can leave the field blank and use the below steps for manually generating a token in the Android Studio terminal. Once your data is entered, rename this file to `env`. 

> :warning: **Do not store credentials in plain text on production environments**

## Configuration

For manually generating a JWT Token:
1. Ensure you are using JDK 16+ and your project language level is set to JDK 16+
2. In the terminal navigate to the utils folder with this command `cd app/src/main/java/com/zoomvideosdkkotlin/utils`
3. Compile the `TokenGenerator.java` with this command: `javac -cp "lib/*"  TokenGenerator.java`
4. Execute the file with this command with command line argument in this order: `java -cp "lib/*" TokenGenerator.java [topic] [role(0 or 1)] [sdk key] [sdk secret]`
5. A JWT Token will output to the console. From there Build and run the application and input the JWT into the app when prompted.

If you choose to use the apps APIClient, the request query parameters and body structure can be edited to match your servers requirements in the `ApiService.kt` file. The current request structure is as follows:
```
curl --location --request POST 'http://ENDPOINT_URL/zoomtoken?token=&name=&password=' \
--header 'Content-Type: application/json' \
--data '{                        
    "body": {                    
        "sessionName" = "",
        "role" = 0,
        "userIdentity" = "",
        "sessionkey" = "",
        "geo_regions" = "",
        "cloud_recording_option" = 0,
        "cloud_recording_election" = 0,
        "telemetry_tracking_id" = "",
        "video_webrtc_mode" = 0,
        "audio_webrtc_mode" = 0
    }
}'
```

## Usage
1. Once your app is built, run it in Android Studio

2. Enter the sessionName and Username in the UI form. Optionally, you can enter password if needed. If you specified an ``ENDPOINT_URL`` in the `env` file, the app will query your endpoint to retrieve a JWT token. If you did not specify an ``ENDPOINT_URL`` in the `env` file, you can leave the JWT Token field blank.

3. Click "Register Session" and the app will initialize and listen for a push notification to trigger the call flow

4. open the terminal and run this command to send a push notification to your app:

`adb shell am broadcast -a com.videosdkconnectionservice.ACTION_START_CALL -n com.videosdkconnectionservice/com.videosdkconnectionservice.receivers.StartCallReceiver`

5. Your app should now receive a call via the android call UI. From there, you can answer and the Zoom Session will start. 
