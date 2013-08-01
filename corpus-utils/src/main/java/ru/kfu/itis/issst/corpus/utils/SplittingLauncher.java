/**
 * 
 */
package ru.kfu.itis.issst.corpus.utils;

import static java.lang.System.exit;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Rinat Gareev (Kazan Federal University)
 * 
 */
public class SplittingLauncher {

	private static final String CMD_LINE_SYNTAX = "split.sh [OPTIONS] corpus-dir";
	private static final String OPT_ANNOTATOR_NUM = "annotatorNum";
	private static final String OPT_ANNOTATORS_PER_DOC = "annotatorsPerDoc";
	private static final String OPT_HELP = "help";

	private static final int DEFAULT_ANNOTATORS_PER_DOC = 2;

	private static final Logger log = LoggerFactory.getLogger(SplittingLauncher.class);

	public static void main(String[] args) throws IOException {
		Option annotatorNumOpt = new Option(OPT_ANNOTATOR_NUM, true, "number of annotators");
		annotatorNumOpt.setType(Number.class);
		annotatorNumOpt.setRequired(true);
		Option annPerDocOpt = new Option(OPT_ANNOTATORS_PER_DOC, true,
				"number of annotators per document");
		annPerDocOpt.setType(Number.class);
		Option helpOpt = new Option(OPT_HELP, false, "Print usage");

		Options opts = new Options();
		opts.addOption(annotatorNumOpt);
		opts.addOption(annPerDocOpt);
		opts.addOption(helpOpt);

		CommandLineParser cmdParser = new GnuParser();
		CommandLine cl = null;
		Number annotatorNum = null;
		Number annPerDoc = null;
		try {
			cl = cmdParser.parse(opts, args);
			annotatorNum = (Number) cl.getParsedOptionValue(OPT_ANNOTATOR_NUM);
			annPerDoc = (Number) cl.getParsedOptionValue(OPT_ANNOTATORS_PER_DOC);
			if (annPerDoc == null) {
				annPerDoc = DEFAULT_ANNOTATORS_PER_DOC;
			}
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
			System.err.println("Specify corpus directory!");
			new HelpFormatter().printHelp(CMD_LINE_SYNTAX, opts);
			exit(1);
		}
		File corpusDir = new File((String) cl.getArgList().get(0));
		if (!corpusDir.isDirectory()) {
			throw new IllegalArgumentException(String.format("%s is not directory", corpusDir));
		}
		File outputDir = new File(corpusDir, "split");
		log.info("The split will be made in {}", outputDir);
		if (outputDir.isDirectory()) {
			log.info("Cleaning {}", outputDir);
			FileUtils.cleanDirectory(outputDir);
		} else {
			outputDir.mkdirs();
		}
		CorpusSplitting cs = new CorpusSplitting(
				corpusDir, "txt", annotatorNum.intValue(), annPerDoc.intValue());
		List<Set<File>> splitArr = cs.run();
		log.info("Copying files...");
		for (int a = 0; a < splitArr.size(); a++) {
			Set<File> aFiles = splitArr.get(a);
			File aDir = new File(outputDir, String.valueOf(a + 1));
			for (File srcFile : aFiles) {
				String fileName = srcFile.getName();
				File destFile = new File(aDir, fileName);
				FileUtils.copyFile(srcFile, destFile);
				// TODO refactor
				File destAnnFile = new File(aDir, FilenameUtils.getBaseName(fileName) + ".ann");
				FileUtils.touch(destAnnFile);
			}
		}
		log.info("Done.");
	}

}