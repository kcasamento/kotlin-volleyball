package kac.scraper.action

data class ScrapeForNewNYUrbanGames(val corrId: String, val url: String, val params: List<Map<String, String>>)