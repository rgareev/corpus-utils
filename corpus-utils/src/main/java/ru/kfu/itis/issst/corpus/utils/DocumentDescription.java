/**
 * 
 */
package ru.kfu.itis.issst.corpus.utils;


/**
 * @author Rinat Gareev (Kazan Federal University)
 * 
 */
public abstract class DocumentDescription {
	private Long id;
	private String uri;
	private int txtLength;

	protected DocumentDescription(Long id, String uri, int txtLength) {
		this.id = id;
		this.uri = uri;
		this.txtLength = txtLength;
	}

	public Long getId() {
		return id;
	}

	public String getUri() {
		return uri;
	}

	public int getTxtLength() {
		return txtLength;
	}
}