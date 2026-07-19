package by.tigre.music.player.core.data.catalog.art

import by.tigre.music.player.core.data.catalog.ArtistArtRemote
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal class DeezerArtistArtRemote(
    private val httpClient: HttpClient,
) : ArtistArtRemote {

    override suspend fun findImageUrl(normalizedName: String): String? {
        val response = httpClient.get("https://api.deezer.com/search/artist") {
            parameter("q", normalizedName)
            parameter("limit", 5)
        }
        when (response.status) {
            HttpStatusCode.TooManyRequests -> throw ArtistArtRateLimitedException()
            else -> {
                if (!response.status.isSuccess()) {
                    throw ArtistArtHttpException(response.status.value)
                }
            }
        }
        val body: DeezerArtistSearchResponse = response.body()
        val artists = body.data
        if (artists.isEmpty()) return null

        val exact = artists.firstOrNull { it.name?.lowercase() == normalizedName }
        val chosen = exact ?: artists.first()
        val url = chosen.pictureMedium?.takeIf { it.isNotBlank() }
            ?: chosen.picture?.takeIf { it.isNotBlank() }
        return url
    }

    @Serializable
    private data class DeezerArtistSearchResponse(
        val data: List<DeezerArtistDto> = emptyList(),
    )

    @Serializable
    private data class DeezerArtistDto(
        val name: String? = null,
        val picture: String? = null,
        @SerialName("picture_medium") val pictureMedium: String? = null,
    )
}

internal class ArtistArtRateLimitedException : Exception("Artist art remote rate limited")
internal class ArtistArtHttpException(val code: Int) : Exception("Artist art HTTP $code")
