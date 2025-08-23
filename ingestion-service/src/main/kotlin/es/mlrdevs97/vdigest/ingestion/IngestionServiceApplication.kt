package es.mlrdevs97.vdigest.ingestion

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class IngestionServiceApplication

fun main(args: Array<String>) {
	runApplication<IngestionServiceApplication>(*args)
}
