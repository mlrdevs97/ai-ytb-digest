package es.mlrdevs97.vdigest.ingestion.service

import es.mlrdevs97.vdigest.ingestion.model.VideoTranscriptReadyEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.util.UUID
import java.util.concurrent.ExecutorService
import kotlin.io.path.fileSize
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readLines

@Service
class TranscriptService(
    private val kafkaTemplate: KafkaTemplate<String, VideoTranscriptReadyEvent>,
    private val executor: ExecutorService
) {
    private val log = LoggerFactory.getLogger(TranscriptService::class.java)

    /**
     * Schedules a background job that downloads captions,
     * parses them, and publishes a VideoTranscriptReadyEvent.
     */
    fun enqueueIngestion(youtubeUrl: String): String {
        val requestId = UUID.randomUUID().toString()
        executor.submit {
            try {
                val transcript = extractTranscript(youtubeUrl)
                val event = VideoTranscriptReadyEvent(requestId, youtubeUrl, transcript)
                kafkaTemplate.send("video.transcripts", requestId, event)
                log.info("Published transcript for requestId={}", requestId)
            } catch (ex: Exception) {
                log.error("Failed to ingest url={}, requestId={}, cause={}", youtubeUrl, requestId, ex.toString(), ex)
            }
        }
        return requestId
    }
    /**
     * Gets a plain-text transcript for a YouTube URL.
     * Downloads .vtt via yt-dlp into a temp folder, parses, and cleans it.
     */
    fun extractTranscript(youtubeUrl: String): String {
        val tempDir = Files.createTempDirectory("yt-vtt-")
        return try {
            val vtt = downloadVtt(youtubeUrl, tempDir)
            if (vtt == null) {
                throw IllegalStateException("No .vtt subtitles found for URL: $youtubeUrl")
            }
            val text = parseVttToText(vtt)
            text.ifBlank { throw IllegalStateException("Parsed transcript is empty for URL: $youtubeUrl") }
        } finally {
            runCatching { tempDir.toFile().deleteRecursively() }
                .onFailure { log.warn("Failed to cleanup temp dir {}", tempDir, it) }
        }
    }

    /**
     * Runs yt-dlp to fetch subtitles.
     */
    private fun downloadVtt(youtubeUrl: String, workDir: Path): Path? {
        val cmd = listOf(
            "yt-dlp",
            "--skip-download",
            "--write-auto-subs", "--write-subs",
            "--sub-langs", "en",
            "--sub-format", "vtt/best",
            "-o", "%(id)s.%(ext)s",
            youtubeUrl
        )

        log.info("Executing yt-dlp to fetch subtitles: {}", cmd.joinToString(" "))
        val pb = ProcessBuilder(cmd)
            .directory(workDir.toFile())
            .redirectErrorStream(true)

        val process = pb.start()
        BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8)).use { r ->
            r.lines().forEach { line -> log.debug("[yt-dlp] {}", line) }
        }

        val exit = process.waitFor()
        if (exit != 0) {
            throw IllegalStateException("yt-dlp exited with code $exit for $youtubeUrl")
        }

        val vtts = workDir.listDirectoryEntries("*.vtt")
        return vtts.maxByOrNull { it.fileSize() }
    }

    /**
     * .vtt → plain text:
     * - Drop WEBVTT header, NOTE/STYLE/REGION blocks, cue timings, and indices
     * - Strip HTML-like tags and common entities
     * - Collapse whitespace
     */
    fun parseVttToText(vttPath: Path): String {
        val lines = vttPath.readLines()
        val builder = StringBuilder()

        var skipBlock = false
        for (raw in lines) {
            var line = raw.trim()

            if (line.isBlank()) continue
            if (line.equals("WEBVTT", ignoreCase = true)) continue

            // NOTE / STYLE / REGION blocks
            if (line.startsWith("NOTE") || line.startsWith("STYLE") || line.startsWith("REGION")) {
                skipBlock = true
                continue
            }
            if (skipBlock) {
                // End block on blank line
                if (raw.isBlank()) skipBlock = false
                continue
            }

            if (TIMING_REGEX.containsMatchIn(line)) continue

            // Pure numeric cue indexes
            if (line.all { it.isDigit() }) continue

            // Remove markup like <c> or <i>...</i>
            line = TAG_REGEX.replace(line, "")

            // Decode a few common HTML entities
            line = htmlEntityDecode(line)

            builder.append(line).append(' ')
        }

        return builder.toString()
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun htmlEntityDecode(s: String): String =
        s.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")

    companion object {
        private val TIMING_REGEX = Regex("\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s+-->\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d{3}")
        private val TAG_REGEX = Regex("<[^>]+>")
    }
}