/**
 * 
 */
package ru.kfu.itis.issst.corpus.utils;

import java.util.Set;
import java.util.SortedSet;

/**
 * @author Rinat Gareev (Kazan Federal University)
 * 
 */
public interface DocumentSet {

	int getSize();

	Set<Object> getAttributeValues(DocumentAttributeKey attrKey);

	int getSizeOfSetWithValue(DocumentAttributeKey attrKey, Object attrVal);

	SortedSet<Long> getIdsOfSetWithValue(DocumentAttributeKey attrKey, Object attrVal);

	DocumentDescription getDocDescription(long id);

	String getDocumentText(long id);
}