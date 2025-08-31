package es.mlrdevs97.vdigest.result.controller

import es.mlrdevs97.vdigest.result.model.VideoDigest
import es.mlrdevs97.vdigest.result.repository.VideoDigestRepository
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/digests")
class DigestQueryController(
    private val repo: VideoDigestRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/{id}")
    fun getById(@PathVariable id: String): ResponseEntity<VideoDigest> {
        log.debug("Fetch digest by id={}", id)
        return ResponseEntity.of(repo.findById(id))
    }

    @GetMapping
    fun getByRequestId(@RequestParam("requestId") requestId: String): ResponseEntity<VideoDigest> {
        log.debug("Fetch digest by requestId={}", requestId)
        return ResponseEntity.of(repo.findByRequestId(requestId))
    }
}