package app.melova.data.remote.datasource

import app.melova.data.remote.MelovaNetwork
import app.melova.data.remote.api.MelovaApi
import app.melova.data.remote.dto.HealthResponseDto
import app.melova.data.remote.dto.LessonsResponseDto
import app.melova.data.remote.dto.ScoringResponseDto
import okhttp3.RequestBody

class MelovaRemoteDataSourceImpl(
    private val api: MelovaApi = MelovaNetwork.scoringApi
) : MelovaRemoteDataSource {

    override suspend fun getHealth(): HealthResponseDto = api.health()

    override suspend fun downloadTeacherWav() = MelovaNetwork.api.downloadTeacherWav()

    override suspend fun downloadTeacherSyllables() = MelovaNetwork.api.downloadTeacherSyllables()

    override suspend fun getLessons(): LessonsResponseDto = MelovaNetwork.api.lessons()

    override suspend fun downloadLessonIntro(lessonId: String) =
        MelovaNetwork.api.downloadLessonIntro(lessonId)

    override suspend fun downloadSectionAudio(lessonId: String, sectionId: String) =
        MelovaNetwork.api.downloadSectionAudio(lessonId, sectionId)

    override suspend fun scorePerformance(body: RequestBody): ScoringResponseDto =
        api.score(body)
}
