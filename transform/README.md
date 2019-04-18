# Transformer Service
Takes the list of games from a previous crawl and filters them out of the games found during the current crawl to determine if any new spots have opened to further be sent to a notification service (email, text, etc).

##Consumes (Topic::Event)
kac.scraper.event::ScrapeFinished

##Produces (Topic::Event)
kac.scraper.event::NewGamesOpened