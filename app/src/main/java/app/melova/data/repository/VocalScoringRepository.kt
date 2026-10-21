package app.melova.data.repository

import app.melova.audio.playback.ExpectedSegment
import app.melova.data.model.VocalScoreResult
import java.io.File

interface VocalScoringRepository {
    suspend fun score(
        referenceFile: File,
        studentFile: File,
        expectedSegments: List<ExpectedSegment>? = null,
        /** When set, the server scores against its own copy of this lesson section. */
        lessonId: String? = null,
        sectionId: String? = null
    ): VocalScoreResult
}
