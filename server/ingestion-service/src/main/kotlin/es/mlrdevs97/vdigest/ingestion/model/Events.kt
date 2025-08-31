package es.mlrdevs97.vdigest.ingestion.model

data class VideoTranscriptReadyEvent(
    val requestId: String,
    val youtubeUrl: String,
    val transcript: String
)
