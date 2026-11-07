package app.melova.data.remote.datasource

import app.melova.data.remote.dto.CatalogResponseDto
import app.melova.data.remote.dto.HealthResponseDto
import app.melova.data.remote.dto.ScoringResponseDto
import app.melova.data.remote.dto.TakePieceAnswerDto
import app.melova.data.remote.dto.TeacherOrnamentsResponseDto
import io.ktor.http.content.OutgoingContent

interface MelovaRemoteDataSource {
    suspend fun getHealth(): HealthResponseDto

    suspend fun downloadTeacherWav(): ByteArray

    suspend fun downloadTeacherSyllables(): ByteArray

    suspend fun getCatalog(teacherToken: String?): CatalogResponseDto

    suspend fun downloadLessonIntro(lessonId: String, version: String): ByteArray

    suspend fun downloadSectionAudio(lessonId: String, sectionId: String, version: String, teacherToken: String?): ByteArray

    suspend fun downloadSectionSlowAudio(lessonId: String, sectionId: String, version: String, speed: Float): ByteArray

    suspend fun getSectionOrnaments(lessonId: String, sectionId: String): TeacherOrnamentsResponseDto

    suspend fun scorePerformance(body: OutgoingContent): ScoringResponseDto

    suspend fun streamTakePiece(streamId: String, body: OutgoingContent): TakePieceAnswerDto

    suspend fun dropTakeStream(streamId: String)
}
