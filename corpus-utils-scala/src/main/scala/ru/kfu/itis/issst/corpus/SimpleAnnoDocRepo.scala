/**
 *
 */
package ru.kfu.itis.issst.corpus

import java.io.File
import org.apache.commons.io.FileUtils
import scala.collection.{ mutable => mu }

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class SimpleAnnoDocRepo(baseDir: File) extends AnnoDocRepo {
  require(baseDir.isDirectory(), s"$baseDir is not an existing directory")

  val annotatorDocs: mu.Map[String, mu.Map[String, AnnoDoc]] = mu.Map.empty //
  init()

  private[this] def init() {
    import scala.collection.JavaConversions._
    for (corpusDir <- baseDir.listFiles(CorpusDirFilter)) {
      val annotatorId = corpusDir.getName()
      val annotatorMap = mu.Map.empty[String, AnnoDoc]
      annotatorDocs(annotatorId) = annotatorMap
      annotatorMap ++= (for {
        annoFile <- corpusDir.listFiles(AnnotationFilenameFilter)
        annoDoc = getAnnoDoc(annoFile, annotatorId)
      } yield (annoDoc.id, annoDoc))
    }
  }

  def getAnnotatorIds(): Set[String] = annotatorDocs.keySet.toSet

  def getDocsAnnotatedBy(annotatorId: String): List[AnnoDoc] = annotatorDocs.get(annotatorId) match {
    case None => throw new IllegalArgumentException(s"Unknown annotatorId : $annotatorId")
    case Some(docs) => docs.values.toList
  }

  def getDoc(id: String, annotatorId: String): Option[AnnoDoc] = annotatorDocs.get(annotatorId) match {
    case None => None
    case Some(id2Doc) => id2Doc.get(id)
  }

  def add(srcDoc: AnnoDoc) {
    val annotatorId = srcDoc.annotatedBy
    getDoc(srcDoc.id, annotatorId) match {
      case Some(alt) => throw new IllegalArgumentException(
        s"Can't add $srcDoc because this repo already has $alt")
      case None =>
        val targetDir = getAnnotatorDir(annotatorId)
        val targetTxtFile = new File(targetDir, getTxtFilename(srcDoc))
        val targetAnnFile = new File(targetDir, getAnnFilename(srcDoc))
        FileUtils.copyFile(srcDoc.txtFile, targetTxtFile, true)
        FileUtils.copyFile(srcDoc.annFile, targetAnnFile, true)
        val newDoc = new AnnoDoc(srcDoc.id, annotatorId, targetTxtFile, targetAnnFile, srcDoc.errorTag)
        val annotIdDocs = annotatorDocs.get(annotatorId) match {
          case None =>
            val x = mu.Map.empty[String, AnnoDoc]
            annotatorDocs(annotatorId) = x
            // copy annotation.conf
            initializeAnnotatorDir(annotatorId, getAnnotationConfFile(srcDoc))
            x
          case Some(x) => x
        }
        annotIdDocs(newDoc.id) = newDoc
        // check modification time
        if (newDoc.txtFile.lastModified() != srcDoc.txtFile.lastModified()
          || newDoc.annFile.lastModified() != srcDoc.annFile.lastModified())
          throw new IllegalStateException(
            s"Modification date has not been preserved:\n$srcDoc,\n$newDoc");
    }
  }

  def update(srcDoc: AnnoDoc) {
    getDoc(srcDoc.id, srcDoc.annotatedBy) match {
      case None => throw new IllegalStateException(s"Illegal update invocation: $srcDoc")
      case Some(oldDoc) =>
        FileUtils.copyFile(srcDoc.annFile, oldDoc.annFile, true)
        if (oldDoc.annFile.lastModified() != srcDoc.annFile.lastModified())
          throw new IllegalStateException(
            s"Modification date has not been preserved:\n$srcDoc,\n$oldDoc");
    }
  }

  private def initializeAnnotatorDir(annotatorId: String, annotConfFile: File) {
    val dir = getAnnotatorDir(annotatorId)
    val targetAnnoConfFile = new File(dir, AnnotationConfFilename)
    if (targetAnnoConfFile.isFile())
      throw new IllegalStateException(s"Dir $dir has been already initialized")
    FileUtils.copyFile(annotConfFile, targetAnnoConfFile, true)
  }

  private def getAnnotatorDir(annotatorId: String) = new File(baseDir, annotatorId)
}