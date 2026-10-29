package app.melova.audio.capture

expect object DeviceAudioHints {
    /** True on the Android emulator, whose mic is off until host audio input is turned on. */
    val isEmulator: Boolean
}
