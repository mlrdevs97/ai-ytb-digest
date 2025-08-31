package es.mlrdevs97.vdigest.result.repository

import es.mlrdevs97.vdigest.result.model.VideoDigest
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.Optional

interface VideoDigestRepository : MongoRepository<VideoDigest, String> {
    fun findByRequestId(requestId: String): Optional<VideoDigest>
}