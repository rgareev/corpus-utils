/**
 * 
 */
package ru.kfu.itis.issst.corpus.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

/**
 * @author Rinat Gareev (Kazan Federal University)
 * 
 */
public class CorpusSplitting {

	private final File inputDir;
	private final String fileNameExt;
	private final int annotatorNum;
	private final int annotatorsPerDocument;

	private final Logger log = LoggerFactory.getLogger(getClass());

	public CorpusSplitting(File inputDir, String fileNameExt, int annotatorNum,
			int annotatorsPerDocument) {
		this.inputDir = inputDir;
		if (!inputDir.isDirectory()) {
			throw new IllegalArgumentException(String.format("%s is not directory", inputDir));
		}
		this.fileNameExt = fileNameExt;
		this.annotatorNum = annotatorNum;
		if (annotatorNum <= 0) {
			throw new IllegalArgumentException();
		}
		if (annotatorsPerDocument <= 0) {
			throw new IllegalArgumentException();
		}
		this.annotatorsPerDocument = annotatorsPerDocument;
	}

	public List<Set<File>> run() {
		final List<File> corpusFiles = ImmutableList.copyOf(inputDir.listFiles((FilenameFilter)
				FileFilterUtils.suffixFileFilter(fileNameExt)));
		if (corpusFiles.isEmpty()) {
			throw new IllegalStateException(String.format(
					"Directory %s does not contain *.%s files",
					inputDir, fileNameExt));
		}
		log.info("There are {} documents in corpus", corpusFiles.size());
		if (corpusFiles.size() % annotatorNum != 0) {
			log.error("{} documents is not divisible evenly by {}", corpusFiles.size(),
					annotatorNum);
		}
		final int avgFileSize = calcAvgSize(corpusFiles);
		if (avgFileSize == 0) {
			throw new IllegalStateException();
		}
		log.info("Average file size: {} bytes", avgFileSize);
		final int bytesPerAnnotator = avgFileSize * annotatorsPerDocument;
		log.info("There are {} bytes per annotator", bytesPerAnnotator);

		Multiset<File> assignmentMS = HashMultiset.create(corpusFiles.size());
		List<Set<File>> splitArr = Lists.newArrayListWithExpectedSize(annotatorNum);
		for (int a = 0; a < annotatorNum; a++) {
			log.info("Getting files for {}-th annotator", a + 1);
			// annotator's set
			Set<File> aSet = Sets.newHashSet();
			int aSetBytes = 0;
			// list of available files
			List<File> avFiles = Lists.newLinkedList();
			for (File f : corpusFiles) {
				if (assignmentMS.count(f) < annotatorsPerDocument) {
					avFiles.add(f);
				}
			}
			Random rand = new Random();
			while (aSetBytes < bytesPerAnnotator && !avFiles.isEmpty()) {
				int fileIdx = rand.nextInt(avFiles.size());
				File file = avFiles.get(fileIdx);
				if (aSet.add(file)) {
					aSetBytes += file.length();
				}
				avFiles.remove(fileIdx);
			}
			log.info("Annotator {} has gotten {} bytes in {} files", new Object[] {
					a + 1, aSetBytes, aSet.size() });
			assignmentMS.addAll(aSet);
			splitArr.add(aSet);
		}
		return splitArr;
	}

	private int calcAvgSize(Iterable<File> corpusFiles) {
		int byteSum = 0;
		for (File f : corpusFiles) {
			byteSum += f.length();
		}
		return byteSum / annotatorNum;
	}
}