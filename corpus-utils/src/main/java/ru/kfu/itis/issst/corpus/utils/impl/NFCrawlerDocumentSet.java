/**
 * 
 */
package ru.kfu.itis.issst.corpus.utils.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.commons.io.IOUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import ru.kfu.itis.issst.corpus.utils.DocumentAttributeKey;
import ru.kfu.itis.issst.corpus.utils.DocumentSet;

/**
 * @author Rinat Gareev (Kazan Federal University)
 * 
 */
public class NFCrawlerDocumentSet extends NamedParameterJdbcDaoSupport implements DocumentSet {

	public static NFCrawlerDocumentSet fromProperties(File propsFile) throws IOException {
		if (!propsFile.isFile()) {
			throw new IllegalArgumentException(String.format("%s does not exist", propsFile));
		}
		Properties props = new Properties();
		InputStream is = new FileInputStream(propsFile);
		try {
			props.load(is);
		} finally {
			IOUtils.closeQuietly(is);
		}
		return fromProperties(props);
	}

	public static NFCrawlerDocumentSet fromProperties(Properties props) {
		int txtMinSize = expectIntProperty(props, "txtMinSize");
		Date pubDateMin = expectDateProperty(props, "pubDateMin");
		Date pubDateMax = expectDateProperty(props, "pubDateMax");
		return new NFCrawlerDocumentSet(txtMinSize, pubDateMin, pubDateMax, props);
	}

	private static final String DATE_PROPERTY_FORMAT = "yyyy-MM-dd HH:mm:ss";

	private static Date expectDateProperty(Properties props, String key) {
		String valStr = expectProperty(props, key);
		valStr = valStr.trim();
		SimpleDateFormat df = new SimpleDateFormat(DATE_PROPERTY_FORMAT);
		try {
			return df.parse(valStr);
		} catch (ParseException e) {
			throw new IllegalArgumentException(String.format(
					"Property '%s' has non-date value: %s", key, valStr), e);
		}
	}

	private static Integer expectIntProperty(Properties props, String key) {
		String valStr = expectProperty(props, key);
		try {
			return Integer.valueOf(valStr);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(String.format(
					"Property '%s' has non-integer value: %s", key, valStr), e);
		}
	}

	private static String expectProperty(Properties props, String key) {
		String valStr = props.getProperty(key);
		if (valStr == null) {
			throw new IllegalArgumentException(String.format("Need value of property '%s'", key));
		}
		return valStr;
	}

	private final int txtMinSize;
	private final Date pubDateMin;
	private final Date pubDateMax;

	public NFCrawlerDocumentSet(int txtMinSize, Date pubDateMin, Date pubDateMax, Properties dsProps) {
		this.txtMinSize = txtMinSize;
		this.pubDateMin = pubDateMin;
		this.pubDateMax = pubDateMax;
		final BasicDataSource ds;
		try {
			ds = (BasicDataSource) BasicDataSourceFactory.createDataSource(dsProps);
			setDataSource(ds);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					ds.close();
				} catch (SQLException e) {
					throw new IllegalStateException(e);
				}
			}
		}));
	}

	@Override
	public int getSize() {
		Map<String, Object> paramMap = prepareParamMap();
		return getNamedParameterJdbcTemplate()
				.queryForObject(SELECT_COUNT, paramMap, Integer.class);
	}

	@Override
	public Set<Object> getAttributeValues(DocumentAttributeKey _attrKey) {
		NFCrawlerDocumentAttributeKey attrKey = (NFCrawlerDocumentAttributeKey) _attrKey;
		switch (attrKey) {
		case FEED:
			return getFeedIds();
		default:
			throw unknownAttributeKey(attrKey);
		}
	}

	private Set<Object> getFeedIds() {
		Map<String, Object> paramMap = prepareParamMap();
		List<Integer> feedIdsList = getNamedParameterJdbcTemplate().queryForList(
				SELECT_FEED_IDS, paramMap, Integer.class);
		return Sets.<Object> newHashSet(feedIdsList);
	}

	@Override
	public int getSizeOfSetWithValue(DocumentAttributeKey _attrKey, Object attrVal) {
		NFCrawlerDocumentAttributeKey attrKey = (NFCrawlerDocumentAttributeKey) _attrKey;
		switch (attrKey) {
		case FEED:
			return getFeedArticlesCount((Integer) attrVal);
		default:
			throw unknownAttributeKey(attrKey);
		}
	}

	private int getFeedArticlesCount(int feedId) {
		Map<String, Object> paramMap = prepareParamMap();
		paramMap.put("feedId", feedId);
		return getNamedParameterJdbcTemplate().queryForObject(
				SELECT_FEED_ARTICLES_COUNT, paramMap, Integer.class);
	}

	@Override
	public SortedSet<Long> getIdsOfSetWithValue(DocumentAttributeKey _attrKey, Object attrVal) {
		NFCrawlerDocumentAttributeKey attrKey = (NFCrawlerDocumentAttributeKey) _attrKey;
		switch (attrKey) {
		case FEED:
			return getFeedArticleIds((Integer) attrVal);
		default:
			throw unknownAttributeKey(attrKey);
		}
	}

	private SortedSet<Long> getFeedArticleIds(int feedId) {
		Map<String, Object> paramMap = prepareParamMap();
		paramMap.put("feedId", feedId);
		return getNamedParameterJdbcTemplate().query(SELECT_FEED_ARTICLE_IDS, paramMap,
				new ResultSetExtractor<SortedSet<Long>>() {
					@Override
					public SortedSet<Long> extractData(ResultSet rs) throws SQLException,
							DataAccessException {
						SortedSet<Long> resultSet = Sets.newTreeSet();
						while (rs.next()) {
							resultSet.add(rs.getLong(1));
						}
						return resultSet;
					}
				});
	}

	@Override
	public NFCrawlerDocumentDescription getDocDescription(final long id) {
		Map<String, Object> paramMap = Maps.newHashMap();
		paramMap.put("id", id);
		return getNamedParameterJdbcTemplate().queryForObject(SELECT_ARTICLE_DESC, paramMap,
				new RowMapper<NFCrawlerDocumentDescription>() {
					@Override
					public NFCrawlerDocumentDescription mapRow(ResultSet rs, int rowNum)
							throws SQLException {
						String uriStr = rs.getString(1);
						int txtLength = rs.getInt(2);
						Date pubDate = null;
						Timestamp pubTS = rs.getTimestamp(3);
						if (pubTS != null) {
							pubDate = new Date(pubTS.getTime());
						}
						int feedId = rs.getInt(4);
						return new NFCrawlerDocumentDescription(id, uriStr, txtLength, pubDate,
								feedId);
					}
				});
	}

	private RuntimeException unknownAttributeKey(NFCrawlerDocumentAttributeKey attrKey) {
		return new UnsupportedOperationException(String.format("Unknown attribute key: %s",
				attrKey));
	}

	private Map<String, Object> prepareParamMap() {
		Map<String, Object> result = Maps.newHashMap();
		result.put("txtMinSize", txtMinSize);
		result.put("pubDateMin", pubDateMin);
		result.put("pubDateMax", pubDateMax);
		return result;
	}

	private static final String SELECT_COUNT =
			"SELECT count(id) FROM article " +
					"WHERE char_length(txt) >= :txtMinSize " +
					"AND pub_date >= :pubDateMin AND pub_date < :pubDateMax";

	private static final String SELECT_FEED_IDS =
			"SELECT DISTINCT feed_id FROM article " +
					"WHERE char_length(txt) >= :txtMinSize " +
					"AND pub_date >= :pubDateMin AND pub_date < :pubDateMax";

	private static final String SELECT_FEED_ARTICLES_COUNT =
			"SELECT count(id) FROM article " +
					"WHERE feed_id = :feedId " +
					"AND char_length(txt) >= :txtMinSize " +
					"AND pub_date >= :pubDateMin AND pub_date < :pubDateMax";

	private static final String SELECT_FEED_ARTICLE_IDS =
			"SELECT id FROM article " +
					"WHERE feed_id = :feedId " +
					"AND char_length(txt) >= :txtMinSize " +
					"AND pub_date >= :pubDateMin AND pub_date < :pubDateMax";

	private static final String SELECT_ARTICLE_DESC =
			"SELECT url, char_length(txt), pub_date, feed_id FROM article WHERE id = :id";
}