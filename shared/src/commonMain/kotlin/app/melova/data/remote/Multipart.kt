package app.melova.data.remote

import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

/** A file sent as one part of a multipart form, like Retrofit's MultipartBody.Part. */
class FilePart(
    val name: String,
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray
)

/** Text fields (nulls left out) and files in one multipart/form-data body. */
fun multipartBody(fields: Map<String, String?>, files: List<FilePart> = emptyList()) = MultiPartFormDataContent(
    formData {
        fields.forEach { (name, value) -> if (value != null) append(name, value) }
        files.forEach { part ->
            append(
                part.name,
                part.bytes,
                Headers.build {
                    append(HttpHeaders.ContentType, part.mimeType)
                    append(HttpHeaders.ContentDisposition, "filename=\"${part.fileName.replace("\"", "")}\"")
                }
            )
        }
    }
)
