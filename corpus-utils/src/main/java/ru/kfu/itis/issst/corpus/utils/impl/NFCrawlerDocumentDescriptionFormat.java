/**
 * 
 */
package ru.kfu.itis.issst.corpus.utils.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.Sets;

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

	public Set<Long> readIdsFrom(File inFile) throws IOException {
		Set<Long> resultSet = Sets.newLinkedHashSet();
		InputStream is = new FileInputStream(inFile);
		BufferedReader in = new BufferedReader(new InputStreamReader(is, "utf-8"));
		try {
			String line;
			int lineNum = 0;
			while ((line = in.readLine()) != null) {
				lineNum++;
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				int commentStart = line.indexOf('#');
				if (commentStart >= 0) {
					line = line.substring(0, commentStart);
					line = line.trim();
				}
				try {
					Long id = Long.valueOf(line);
					resultSet.add(id);
				} catch (NumberFormatException e) {
					throw new IllegalStateException("On line " + lineNum, e);
				}
			}
		} finally {
			IOUtils.closeQuietly(in);
		}
		return resultSet;

	}
}