package app.melova.audio.capture

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build

object BuiltInMicrophoneSelector {
    fun preferBuiltInMicrophone(context: Context, recorder: AudioRecord): Boolean {
        if (DeviceAudioHints.isEmulator) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false

        val builtInMic = findBuiltInMicrophone(context) ?: return false
        return recorder.setPreferredDevice(builtInMic)
    }

    fun findBuiltInMicrophone(context: Context): AudioDeviceInfo? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
    }

    fun chooseAudioSource(context: Context): Int {
        if (DeviceAudioHints.isEmulator) {
            return MediaRecorder.AudioSource.VOICE_RECOGNITION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return MediaRecorder.AudioSource.MIC
        }
        return if (isHeadsetConnected(context)) {
            MediaRecorder.AudioSource.CAMCORDER
        } else {
            MediaRecorder.AudioSource.MIC
        }
    }

    fun isHeadsetConnected(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { device ->
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            }
        }

        @Suppress("DEPRECATION")
        return audioManager.isWiredHeadsetOn || audioManager.isBluetoothA2dpOn
    }
}
