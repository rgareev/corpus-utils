/**
 * 
 */
package ru.kfu.itis.issst.corpus.utils.impl;

import java.util.Date;

import ru.kfu.itis.issst.corpus.utils.DocumentDescription;

/**
 * @author Rinat Gareev (Kazan Federal University)
 * 
 */
public class NFCrawlerDocumentDescription extends DocumentDescription {

	private Date pubDate;
	private int feedId;

	public NFCrawlerDocumentDescription(Long id, String uri, int txtLength, Date pubDate, int feedId) {
		super(id, uri, txtLength);
		this.pubDate = pubDate;
		this.feedId = feedId;
	}

	public Date getPubDate() {
		return pubDate;
	}

	public int getFeedId() {
		return feedId;
	}
}