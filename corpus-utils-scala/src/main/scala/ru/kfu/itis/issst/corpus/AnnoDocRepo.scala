/**
 *
 */
package ru.kfu.itis.issst.corpus

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
trait AnnoDocRepo {

  def getAnnotatorIds(): Set[String]

  def getDocsAnnotatedBy(annotatorId: String): List[AnnoDoc]

  def getDoc(id: String, annotatorId: String): Option[AnnoDoc]

  /**
   * Add the provided document to this repository by COPYING its content, preserving ids, timestamps and tags
   */
  def add(doc: AnnoDoc)

  /**
   * Update annotations
   */
  def update(doc: AnnoDoc)
}
