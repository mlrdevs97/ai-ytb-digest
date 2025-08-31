package es.mlrdevs97.vdigest.aiprocessor.model

data class VideoTranscriptReadyEvent(
    val requestId: String,
    val youtubeUrl: String,
    val transcript: String
)

data class DigestCompletedEvent(
    val requestId: String,
    val youtubeUrl: String,
    val summary: String,
    val tags: List<String>,
)

