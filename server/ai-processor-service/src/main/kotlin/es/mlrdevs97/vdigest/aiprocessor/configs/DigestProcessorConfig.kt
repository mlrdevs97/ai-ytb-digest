package es.mlrdevs97.vdigest.aiprocessor.configs

import es.mlrdevs97.vdigest.aiprocessor.model.DigestCompletedEvent
import es.mlrdevs97.vdigest.aiprocessor.model.VideoTranscriptReadyEvent
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatOptions
import java.util.function.Function

@Configuration
class DigestProcessorConfig(
    private val chatClientBuilder: ChatClient.Builder
) {
    private val log = LoggerFactory.getLogger(DigestProcessorConfig::class.java)

    /**
     * Consumes VideoTranscriptReadyEvent, calls the LLM, and emits DigestCompletedEvent.
     */
    @Bean
    fun digestProcessor(): Function<VideoTranscriptReadyEvent, DigestCompletedEvent> =
        Function { event ->
            log.info("Processing transcript requestId={} url={}", event.requestId, event.youtubeUrl)

            val chat = chatClientBuilder.build()

            val normalized = event.transcript.trim().replace(Regex("\\s+"), " ")
            val useSinglePass = normalized.length <= MAX_SINGLE_PASS_CHARS

            val result = if (useSinglePass) {
                callDigest(chat, normalized)
            } else {
                val bullets = summarizeInChunks(chat, normalized)
                callDigest(chat, bullets.joinToString("\n"))
            }

            DigestCompletedEvent(
                requestId = event.requestId,
                youtubeUrl = event.youtubeUrl,
                summary = result.summary.trim(),
                tags = result.tags.map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct().take(10),
            )
        }


    private fun summarizeInChunks(chat: ChatClient, text: String): List<String> {
        val chunks = chunk(text)
        return chunks.mapIndexed { idx, chunk ->
            val prompt = """
                Summarize the following transcript chunk into at most 8 concise bullet points.
                Use short, factual bullets; no emojis; no markdown; no preface.

                Chunk #${idx + 1}:
                $chunk
            """.trimIndent()

            val raw = chat.prompt()
                .user(prompt)
                .call()
                .content()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("LLM returned empty content for chunk ${idx + 1}")

            raw.lineSequence()
                .map { it.trim() }
                .map { it.removePrefix("-").removePrefix("*").removePrefix("•").trim() }
                .map { it.replace(Regex("""^\d+[.)\-\s]+"""), "").trim() }
                .filter { it.isNotBlank() }
                .toList()
        }.flatten()
    }

    private fun callDigest(chat: ChatClient, text: String): DigestLLMResponse {
        val system = """
            You are an assistant that produces compact digests for YouTube transcripts.
            Your response MUST be a single JSON object with this exact schema:
            {
              "summary": string,
              "tags": string[],
            }
            Do NOT add any extra text, code fences, or commentary.
        """.trimIndent()

        val user = """
            Transcript content:
            $text
        """.trimIndent()

        return chat.prompt()
            .system(system)
            .user(user)
            .call()
            .entity(DigestLLMResponse::class.java)
            ?: throw IllegalStateException(
                "LLM returned no JSON or parsing failed."
            )
    }

    private fun chunk(text: String): List<String> {
        if (text.length <= CHUNK_SIZE_CHARS) return listOf(text)
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            val end = (start + CHUNK_SIZE_CHARS).coerceAtMost(text.length)
            chunks += text.substring(start, end)
            if (end == text.length) break
            start = (end - OVERLAP_CHARS).coerceAtLeast(0)
        }
        return chunks
    }

    /**
     * Internal representation of the response from the LLM.
     */
    private data class DigestLLMResponse(
        val summary: String,
        val tags: List<String>,
    )

    companion object {
        private const val MAX_SINGLE_PASS_CHARS = 12_000
        private const val CHUNK_SIZE_CHARS = 5_000
        private const val OVERLAP_CHARS = 400
    }
}
