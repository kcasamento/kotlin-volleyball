package kac.crawl.event

import java.util.*

data class CrawlJobCompleted (val id: String, val message: String = "", val completeTime: Date)