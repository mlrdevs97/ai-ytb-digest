package es.mlrdevs97.vdigest.result.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("video_digests")
data class VideoDigest(
    @Id
    val id: String,
    val requestId: String,
    val youtubeUrl: String,
    val summary: String,
    val tags: List<String>,
    val sentiment: String,
    val createdAt: Instant = Instant.now()
)

