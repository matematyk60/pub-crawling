package com.guys.coding.hackathon.backend.domain

object UrlFilter {
  val begs = List(
    "https://duckduckgo.com/y.js",
    "https://www.youtube.com/", // unlikely to match
    "https://pl.wikipedia.org", // not likely to contain phone numbers etc
    "https://en.wikipedia.org",
    "https://translate.google.com", // don't do that
    "https://webcache.googleusercontent.com",
    "https://support.google.com",
    "https://policies.google.com",
    "https://developers.google.com"
  )

  val wholes = Set( //   will generate many faklse positivers
    "https://pogoda.onet.pl",
    "http://gry.onet.pl",
    "https://www.onet.pl",
    "https://www.wp.pl",
    "https://www.wykop.pl/hity",
    "https://www.wykop.pl/mikroblog",
    "https://www.twitter.com"
  )

  def allowedToCrawl(url: String)    = !dontCrawl(url)
  private def dontCrawl(url: String) = begs.exists(start => url.startsWith(start)) || wholes.contains(url)

}
