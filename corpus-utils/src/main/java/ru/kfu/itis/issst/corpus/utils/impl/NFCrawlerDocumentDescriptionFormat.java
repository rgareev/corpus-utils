/**
 * 
 */
package ru.kfu.itis.issst.corpus.utils.impl;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

import org.apache.commons.io.IOUtils;

/**
 * @author Rinat Gareev (Kazan Federal University)
 * 
 */
public class NFCrawlerDocumentDescriptionFormat {
	public void writeTo(Iterable<NFCrawlerDocumentDescription> src, String outputFilePath)
			throws IOException {
		OutputStream os = new FileOutputStream(outputFilePath);
		Writer writer = new BufferedWriter(new OutputStreamWriter(os, "utf-8"));
		PrintWriter out = new PrintWriter(writer, true);
		try {
			for (NFCrawlerDocumentDescription doc : src) {
				out.println(String.format(
						"%1$s # feed_id=%2$s,pub_date=%3$tY-%3$tm-%3$td,txt_length=%4$s,url=%5$s",
						doc.getId(),
						doc.getFeedId(), doc.getPubDate(), doc.getTxtLength(), doc.getUri()));
			}
		} finally {
			IOUtils.closeQuietly(out);
		}
	}
}