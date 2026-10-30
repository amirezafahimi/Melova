package app.melova.data.remote.mapper

import app.melova.data.model.Catalog
import app.melova.data.model.Lesson
import app.melova.data.model.LessonPayment
import app.melova.data.model.LessonPurchaseStatus
import app.melova.data.model.LessonSection
import app.melova.data.model.Teacher
import app.melova.data.model.TeacherProfile
import app.melova.data.remote.dto.CatalogResponseDto
import app.melova.data.remote.dto.LessonDto
import app.melova.data.remote.dto.TeacherProfileDto

fun CatalogResponseDto.toDomain(): Catalog = Catalog(
    teachers = teachers.orEmpty().mapNotNull { dto ->
        val teacherId = dto.id?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        Teacher(id = teacherId, name = dto.name.orEmpty(), bio = dto.bio.orEmpty())
    },
    lessons = lessons.orEmpty().mapNotNull { it.toDomain() },
    followedTeacherIds = followedTeachers?.toSet()
)

fun TeacherProfileDto.toDomain(): TeacherProfile = TeacherProfile(
    id = id.orEmpty(),
    name = name.orEmpty(),
    bio = bio.orEmpty(),
    cardNumber = cardNumber.orEmpty(),
    cardHolder = cardHolder.orEmpty(),
    lessons = lessons.orEmpty().mapNotNull { it.toDomain() }
)

private fun LessonDto.toDomain(): Lesson? {
    val lessonId = id?.takeIf { it.isNotBlank() } ?: return null
    return Lesson(
        id = lessonId,
        teacherId = teacherId.orEmpty(),
        teacher = teacher.orEmpty(),
        title = title.orEmpty(),
        subtitle = subtitle.orEmpty(),
        description = description.orEmpty(),
        level = level.orEmpty(),
        published = published == true,
        hasIntro = hasIntro == true,
        introVersion = introVersion.orEmpty(),
        isFree = isFree != false,
        priceToman = priceToman ?: 0L,
        bazaarSku = bazaarSku.orEmpty(),
        isOwned = isOwned,
        purchaseStatus = LessonPurchaseStatus.fromRaw(purchaseStatus),
        payment = payment?.cardNumber?.takeIf { it.isNotBlank() }?.let {
            LessonPayment(cardNumber = it, cardHolder = payment.cardHolder.orEmpty())
        },
        sections = sections.orEmpty().mapNotNull { dto ->
            val sectionId = dto.id?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            LessonSection(
                id = sectionId,
                title = dto.title.orEmpty(),
                description = dto.description.orEmpty(),
                isAvailable = dto.isAvailable == true,
                durationMs = dto.durationMs ?: 0L,
                audioVersion = dto.audioVersion.orEmpty()
            )
        }
    )
}
