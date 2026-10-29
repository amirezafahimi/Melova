package app.melova.data.remote.datasource

import app.melova.data.remote.dto.HealthResponseDto
import app.melova.data.remote.dto.CatalogResponseDto
import app.melova.data.remote.dto.ScoringResponseDto
import app.melova.data.remote.dto.TeacherOrnamentsResponseDto
import okhttp3.RequestBody
import okhttp3.ResponseBody

interface MelovaRemoteDataSource {
    suspend fun getHealth(): HealthResponseDto

    suspend fun downloadTeacherWav(): ResponseBody

    suspend fun downloadTeacherSyllables(): ResponseBody

    suspend fun getCatalog(teacherToken: String?): CatalogResponseDto

    suspend fun downloadLessonIntro(lessonId: String): ResponseBody

    suspend fun downloadSectionAudio(lessonId: String, sectionId: String): ResponseBody

    suspend fun getSectionOrnaments(lessonId: String, sectionId: String): TeacherOrnamentsResponseDto

    suspend fun scorePerformance(body: RequestBody): ScoringResponseDto
}
