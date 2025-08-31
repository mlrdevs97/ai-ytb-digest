package es.mlrdevs97.vdigest.ingestion.controller

import es.mlrdevs97.vdigest.ingestion.service.TranscriptService
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/api/v1/digests")
@Validated
class DigestController(
    private val transcriptService: TranscriptService
) {
    data class CreateDigestRequest(@field:NotBlank val youtubeUrl: String)
    data class CreateDigestResponse(val requestId: String)

    @PostMapping
    fun create(@RequestBody req: CreateDigestRequest): ResponseEntity<CreateDigestResponse> {
        val requestId = transcriptService.enqueueIngestion(req.youtubeUrl)
        val location = URI.create("/api/v1/digests/by-request/$requestId")
        return ResponseEntity.accepted()
            .location(location)
            .body(CreateDigestResponse(requestId))
    }
}
