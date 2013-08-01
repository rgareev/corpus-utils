/**
 * 
 */
package ru.kfu.itis.issst.corpus.utils;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author Rinat Gareev (Kazan Federal University)
 * 
 */
public class SimpleStratifiedSampling {
	private final Logger log = LoggerFactory.getLogger(getClass());

	// config
	private final int sampleSize;
	private final DocumentAttributeKey stratificationAttrKey;
	private final DocumentSet docSet;

	public SimpleStratifiedSampling(int sampleSize, DocumentAttributeKey stratificationAttrKey,
			DocumentSet docSet) {
		this.sampleSize = sampleSize;
		this.stratificationAttrKey = stratificationAttrKey;
		this.docSet = docSet;
	}

	public SortedSet<Long> run() {
		int sourceSetSize = docSet.getSize();
		log.info("There are {} documents in the source set", sourceSetSize);
		Set<Object> saValues = docSet.getAttributeValues(stratificationAttrKey);
		Map<Object, Integer> stratumSampleSizes = Maps.newHashMapWithExpectedSize(saValues.size());
		for (Object sav : saValues) {
			int savSize = docSet.getSizeOfSetWithValue(stratificationAttrKey, sav);
			if (savSize > 0) {
				long stratumSampleSize = Math
						.round(sampleSize * ((double) savSize / sourceSetSize));
				if (stratumSampleSize == 0) {
					stratumSampleSize = 1;
				}
				stratumSampleSizes.put(sav, toInt(stratumSampleSize));
			} else {
				log.info("There are no documents with {}={}", stratificationAttrKey, sav);
			}
		}
		// check fitting into sampleSize
		fitStratumSizes(stratumSampleSizes);
		// result set of sample document IDs
		final TreeSet<Long> sampleSet = Sets.newTreeSet();
		// sample stratum
		for (Object sav : stratumSampleSizes.keySet()) {
			log.info("Sampling strata {}={}...", stratificationAttrKey, sav);
			int strataSampleSize = stratumSampleSizes.get(sav);
			SortedSet<Long> strataSampleSet = sampleStrata(sav, strataSampleSize);
			sampleSet.addAll(strataSampleSet);
			log.info("Sampled {} documents.", strataSampleSet.size());
		}
		// sanityCheck
		if (sampleSet.size() != sampleSize) {
			throw new IllegalStateException();
		}
		return sampleSet;
	}

	private SortedSet<Long> sampleStrata(Object strataAttrVal, int strataSampleSize) {
		SortedSet<Long> strataIds = docSet.getIdsOfSetWithValue(stratificationAttrKey,
				strataAttrVal);
		ArrayList<Long> strataIdsList = Lists.newArrayList(strataIds);
		Random rand = new Random();
		TreeSet<Long> resultSet = Sets.newTreeSet();
		while (resultSet.size() != strataSampleSize) {
			int idIndex = rand.nextInt(strataIdsList.size());
			long id = strataIdsList.get(idIndex);
			resultSet.add(id);
		}
		return resultSet;
	}

	private void fitStratumSizes(final Map<Object, Integer> stratumSampleSizes) {
		int sssSum = 0;
		for (Integer sss : stratumSampleSizes.values()) {
			sssSum += sss;
		}
		final int delta = sampleSize - sssSum;
		if (delta != 0) {
			// search the biggest stratum
			Object biggestStratumAV = null;
			int biggestStratumSize = -1;
			for (Map.Entry<Object, Integer> sssEntry : stratumSampleSizes.entrySet()) {
				if (sssEntry.getValue() > biggestStratumSize) {
					biggestStratumAV = sssEntry.getKey();
					biggestStratumSize = sssEntry.getValue();
				}
			}
			log.info(
					"After proportional allocation of stratum samples the sample size is {}. "
							+ "The sample of the biggest stratum '{}={}' will be {} from {} to {} documents.",
					new Object[] {
							sssSum, stratificationAttrKey, biggestStratumAV,
							delta > 0 ? "extended" : "reduced",
							biggestStratumSize, biggestStratumSize + delta });
			// extend/reduce
			stratumSampleSizes.put(biggestStratumAV, biggestStratumSize + delta);
			// sanity check
			sssSum = 0;
			for (Integer sss : stratumSampleSizes.values()) {
				sssSum += sss;
			}
			if (sssSum != sampleSize) {
				throw new IllegalStateException();
			}
		}
	}

	private static int toInt(long l) {
		if (l > Integer.MAX_VALUE) {
			// should never happen
			throw new IllegalStateException();
		}
		return (int) l;
	}
}