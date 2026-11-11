package app.melova.data.repository

import app.melova.audio.playback.ExpectedSegment
import app.melova.data.model.VocalScoreResult
import app.melova.platform.LocalFile
import kotlinx.coroutines.CoroutineScope

interface VocalScoringRepository {
    suspend fun score(
        referenceFile: LocalFile,
        studentFile: LocalFile,
        expectedSegments: List<ExpectedSegment>? = null,
        /** When set, the server scores against its own copy of this lesson section. */
        lessonId: String? = null,
        sectionId: String? = null,
        /** The [TakeStream] this take was sung into, whose analysis the server has partly done. */
        streamed: TakeStream.Streamed? = null
    ): VocalScoreResult

    /** A take sent while it is sung; null where scoring doesn't support it. */
    fun streamTake(scope: CoroutineScope, lessonId: String?, sectionId: String? = null): TakeStream? = null
}
