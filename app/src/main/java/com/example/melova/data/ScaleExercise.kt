package com.example.melova.data

enum class ScalePattern {
    ASCENDING,
    DESCENDING,
    ASCENDING_DESCENDING
}

enum class ScaleExercise(
    val id: String,
    val title: String,
    val description: String,
    val isMinor: Boolean,
    val pattern: ScalePattern
) {
    MAJOR_ASCENDING(
        id = "major_asc",
        title = "ماژور بالارونده",
        description = "گام دو ماژور از «دو» تا «دو» بالا.",
        isMinor = false,
        pattern = ScalePattern.ASCENDING
    ),
    MAJOR_DESCENDING(
        id = "major_desc",
        title = "ماژور پایین‌رونده",
        description = "گام دو ماژور از «دو» بالا تا «دو» پایین.",
        isMinor = false,
        pattern = ScalePattern.DESCENDING
    ),
    MAJOR_ASCENDING_DESCENDING(
        id = "major_asc_desc",
        title = "ماژور بالا-پایین‌رونده",
        description = "گام دو ماژور: بالا رفتن و برگشت پایین.",
        isMinor = false,
        pattern = ScalePattern.ASCENDING_DESCENDING
    ),
    MINOR_ASCENDING(
        id = "minor_asc",
        title = "مینور بالارونده",
        description = "گام دو مینور طبیعی از «دو» تا «دو» بالا.",
        isMinor = true,
        pattern = ScalePattern.ASCENDING
    ),
    MINOR_DESCENDING(
        id = "minor_desc",
        title = "مینور پایین‌رونده",
        description = "گام دو مینور طبیعی از «دو» بالا تا «دو» پایین.",
        isMinor = true,
        pattern = ScalePattern.DESCENDING
    ),
    MINOR_ASCENDING_DESCENDING(
        id = "minor_asc_desc",
        title = "مینور بالا-پایین‌رونده",
        description = "گام دو مینور طبیعی: بالا رفتن و برگشت پایین.",
        isMinor = true,
        pattern = ScalePattern.ASCENDING_DESCENDING
    );

    companion object {
        val all = entries
    }
}
