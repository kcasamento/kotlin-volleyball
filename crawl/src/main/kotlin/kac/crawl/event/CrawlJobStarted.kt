package kac.crawl.event

import java.util.*

data class CrawlJobStarted(val correlationId: String, val startTimestamp: Date)