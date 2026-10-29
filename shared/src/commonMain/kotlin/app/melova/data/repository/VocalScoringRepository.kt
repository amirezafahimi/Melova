package app.melova.data.repository

import app.melova.audio.playback.ExpectedSegment
import app.melova.data.model.VocalScoreResult
import app.melova.platform.LocalFile

interface VocalScoringRepository {
    suspend fun score(
        referenceFile: LocalFile,
        studentFile: LocalFile,
        expectedSegments: List<ExpectedSegment>? = null,
        /** When set, the server scores against its own copy of this lesson section. */
        lessonId: String? = null,
        sectionId: String? = null
    ): VocalScoreResult
}
