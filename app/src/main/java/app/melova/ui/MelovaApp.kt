package app.melova.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
fun MelovaApp() {
    var currentScreenName by rememberSaveable { mutableStateOf(AppScreen.Splash.name) }
    val currentScreen = AppScreen.valueOf(currentScreenName)

    when (currentScreen) {
        AppScreen.Splash -> SplashScreen(
            onSolfegeSelected = { currentScreenName = AppScreen.Solfege.name },
            onVocalTrainingSelected = { currentScreenName = AppScreen.VocalTraining.name }
        )

        AppScreen.Solfege -> MelovaScreen(
            onBack = { currentScreenName = AppScreen.Splash.name }
        )

        AppScreen.VocalTraining -> VocalTrainingScreen(
            onBack = { currentScreenName = AppScreen.Splash.name }
        )
    }
}
