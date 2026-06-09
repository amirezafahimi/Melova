package app.melova.data.model

data class ScoreMetric(
    val id: String,
    val label: String,
    val value: Int,
    val hint: String? = null
)
