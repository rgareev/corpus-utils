/**
 * 
 */
package ru.kfu.itis.issst.corpus.utils;

import static java.lang.System.exit;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.SortedSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import ru.kfu.itis.issst.corpus.utils.impl.NFCrawlerDocumentAttributeKey;
import ru.kfu.itis.issst.corpus.utils.impl.NFCrawlerDocumentDescription;
import ru.kfu.itis.issst.corpus.utils.impl.NFCrawlerDocumentDescriptionFormat;
import ru.kfu.itis.issst.corpus.utils.impl.NFCrawlerDocumentSet;

/**
 * @author Rinat Gareev (Kazan Federal University)
 * 
 */
public class SamplingLauncher {

	public static final String DEFAULT_RESULT_IDS_FILE = "sample-ids.txt";
	public static final String DEFAULT_DS_CONFIG_FILE = "docset-config.properties";

	private static final String CMD_LINE_SYNTAX = "sample.sh [OPTIONS] [output-file]";
	private static final String OPT_HELP = "help";
	private static final String OPT_SAMPLE_SIZE = "sampleSize";
	private static final String OPT_DS_CONFIG_FILE = "dsConfigFile";

	private static final Logger log = LoggerFactory.getLogger(SamplingLauncher.class);

	public static void main(String[] args) throws ParseException, IOException {
		Option docSetCfgOpt = new Option(OPT_DS_CONFIG_FILE, true,
				"The documents DataSource configuration file");
		docSetCfgOpt.setType(File.class);
		Option sampleSizeOpt = new Option(OPT_SAMPLE_SIZE, true, "The expected sample size");
		sampleSizeOpt.setRequired(true);
		sampleSizeOpt.setType(Number.class);
		Option helpOpt = new Option(OPT_HELP, false, "Print usage");

		Options opts = new Options();
		opts.addOption(sampleSizeOpt);
		opts.addOption(docSetCfgOpt);
		opts.addOption(helpOpt);

		CommandLineParser cmdParser = new GnuParser();
		CommandLine cl = null;
		try {
			cl = cmdParser.parse(opts, args);
		} catch (ParseException e) {
			System.err.println("Command line parsing failed: " + e);
			new HelpFormatter().printHelp(CMD_LINE_SYNTAX, opts);
			exit(1);
		}
		if (cl.hasOption(OPT_HELP)) {
			new HelpFormatter().printHelp(CMD_LINE_SYNTAX, opts);
			exit(1);
		}

		File docSetConfigFile = (File) cl.getParsedOptionValue(OPT_DS_CONFIG_FILE);
		if (docSetConfigFile == null) {
			docSetConfigFile = new File(DEFAULT_DS_CONFIG_FILE);
		}
		Number sampleSize = (Number) cl.getParsedOptionValue(OPT_SAMPLE_SIZE);

		String outputFilePath = null;
		if (cl.getArgList().isEmpty()) {
			outputFilePath = DEFAULT_RESULT_IDS_FILE;
		} else {
			outputFilePath = (String) cl.getArgList().get(0);
		}

		NFCrawlerDocumentSet docSet = NFCrawlerDocumentSet.fromProperties(docSetConfigFile);
		SortedSet<Long> sampleIds = new SimpleStratifiedSampling(sampleSize.intValue(),
				NFCrawlerDocumentAttributeKey.FEED,
				docSet).run();
		log.info("Sampling finished. Fetching sample document descriptions...");
		List<NFCrawlerDocumentDescription> sampleDocs = fetchDocs(docSet, sampleIds);
		log.info("Writing result file {}", outputFilePath);
		new NFCrawlerDocumentDescriptionFormat().writeTo(sampleDocs, outputFilePath);
		log.info("Done.");
	}

	private static List<NFCrawlerDocumentDescription> fetchDocs(NFCrawlerDocumentSet docSet,
			SortedSet<Long> ids) {
		List<NFCrawlerDocumentDescription> docs = Lists
				.newArrayListWithExpectedSize(ids.size());
		long charLengthSum = 0;
		for (long docId : ids) {
			NFCrawlerDocumentDescription doc = docSet.getDocDescription(docId);
			charLengthSum += doc.getTxtLength();
			docs.add(doc);
		}
		final double avgLength = charLengthSum / (double) ids.size();
		log.info("Average doc length: {}", avgLength);
		// calc standard deviation
		double stdDev = 0;
		for (NFCrawlerDocumentDescription doc : docs) {
			stdDev += (doc.getTxtLength() - avgLength) * (doc.getTxtLength() - avgLength);
		}
		stdDev = stdDev / docs.size();
		stdDev = Math.sqrt(stdDev);
		log.info("Standard deviation of txt length: {}", stdDev);
		return docs;
	}
}