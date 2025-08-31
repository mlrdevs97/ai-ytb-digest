package es.mlrdevs97.vdigest.result.service

import es.mlrdevs97.vdigest.result.model.DigestCompletedEvent
import es.mlrdevs97.vdigest.result.model.VideoDigest
import es.mlrdevs97.vdigest.result.repository.VideoDigestRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class DigestResultConsumer(
    private val repo: VideoDigestRepository,
) {
    private val log = LoggerFactory.getLogger(DigestResultConsumer::class.java)

    @KafkaListener(
        topics = ["\${app.kafka.topics.digests-completed:video.digests.completed}"],
        groupId = "\${spring.kafka.consumer.group-id:result-service}"
    )
    fun handle(event: DigestCompletedEvent) {
        log.info("Consuming digest requestId={} url={}", event.requestId, event.youtubeUrl)

        val entity = VideoDigest(
            id = event.requestId,
            requestId = event.requestId,
            youtubeUrl = event.youtubeUrl,
            summary = event.summary.trim(),
            tags = event.tags.map { it.trim() }.filter { it.isNotBlank() },
            sentiment = event.sentiment.lowercase()
        )

        val saved = repo.save(entity)
        log.info("Saved digest id={} requestId={}", saved.id, saved.requestId)
    }
}
