/**
 * 
 */
package ru.kfu.itis.issst.corpus.utils;

import static java.lang.System.exit;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.kfu.itis.issst.corpus.utils.impl.NFCrawlerDocumentDescriptionFormat;
import ru.kfu.itis.issst.corpus.utils.impl.NFCrawlerDocumentSet;

/**
 * @author Rinat Gareev (Kazan Federal University)
 * 
 */
public class DocToFile {

	private static final String CMD_LINE_SYNTAX = "doc-to-file.sh [OPTIONS] output-directory";
	private static final String OPT_DOC_LIST_FILE = "docListFile";
	private static final String OPT_DS_CONFIG_FILE = "dsConfigFile";
	private static final String OPT_HELP = "help";

	private static final Logger log = LoggerFactory.getLogger(DocToFile.class);

	public static void main(String[] args) throws IOException {
		Option docListFileOpt = new Option(OPT_DOC_LIST_FILE, true, "The file listing document IDs");
		docListFileOpt.setType(File.class);
		Option docSetCfgOpt = new Option(OPT_DS_CONFIG_FILE, true,
				"The documents DataSource configuration file");
		docSetCfgOpt.setType(File.class);
		Option helpOpt = new Option(OPT_HELP, false, "Print usage");

		Options opts = new Options();
		opts.addOption(docListFileOpt);
		opts.addOption(docSetCfgOpt);
		opts.addOption(helpOpt);

		CommandLineParser cmdParser = new GnuParser();
		CommandLine cl = null;
		File docListFile = null;
		File docSetCfgFile = null;
		try {
			cl = cmdParser.parse(opts, args);
			docListFile = (File) cl.getParsedOptionValue(OPT_DOC_LIST_FILE);
			docSetCfgFile = (File) cl.getParsedOptionValue(OPT_DS_CONFIG_FILE);
		} catch (ParseException e) {
			System.err.println("Command line parsing failed: " + e);
			new HelpFormatter().printHelp(CMD_LINE_SYNTAX, opts);
			exit(1);
		}
		if (cl.hasOption(OPT_HELP)) {
			new HelpFormatter().printHelp(CMD_LINE_SYNTAX, opts);
			exit(1);
		}
		if (cl.getArgList().isEmpty()) {
			System.err.println("Specify output directory!");
			new HelpFormatter().printHelp(CMD_LINE_SYNTAX, opts);
			exit(1);
		}
		File outputDir = new File((String) cl.getArgList().get(0));
		if (!outputDir.isDirectory()) {
			// create directory
			if (!outputDir.mkdirs()) {
				System.err.println("Could not create directory " + outputDir);
				exit(1);
			}
		}
		if (docListFile == null) {
			docListFile = new File(SamplingLauncher.DEFAULT_RESULT_IDS_FILE);
		}
		expectFile(docListFile);
		if (docSetCfgFile == null) {
			docSetCfgFile = new File(SamplingLauncher.DEFAULT_DS_CONFIG_FILE);
		}
		expectFile(docSetCfgFile);

		NFCrawlerDocumentSet docSet = NFCrawlerDocumentSet.fromProperties(docSetCfgFile);
		Set<Long> docIds = new NFCrawlerDocumentDescriptionFormat().readIdsFrom(docListFile);
		log.info("{} ids have been read", docIds.size());
		log.info("Fetching document texts...");
		saveTexts(docSet, docIds, outputDir);
		log.info("Done.");
	}

	private static void saveTexts(NFCrawlerDocumentSet docSet, Set<Long> ids, File outputDir)
			throws IOException {
		int logPeriod = ids.size() / 10;
		int persisted = 0;
		for (Long id : ids) {
			String txt = docSet.getDocumentText(id);
			if (txt == null) {
				log.warn("No text for article with id={}", id);
				continue;
			}
			File outFile = new File(outputDir, String.format("%s.txt", id));
			FileUtils.writeStringToFile(outFile, txt, "utf-8");
			persisted++;
			if (logPeriod > 0 && persisted % logPeriod == 0) {
				log.info("{} texts have been persisted", persisted);
			}
		}
	}

	private static void expectFile(File f) {
		if (!f.isFile()) {
			System.err.println(String.format("Input file %s does not exist", f));
			exit(1);
		}
	}
}