package es.mlrdevs97.vdigest.result.model

data class DigestCompletedEvent(
    val requestId: String,
    val youtubeUrl: String,
    val summary: String,
    val tags: List<String>,
    val sentiment: String
)