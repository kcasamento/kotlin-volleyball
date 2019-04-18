package kac.scraper.event

data class ScraperErrored(val exception: Exception, val friendlyMessage: String = "")