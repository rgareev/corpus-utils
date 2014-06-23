/**
 *
 */
package ru.kfu.itis.issst

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
import java.io.File
import org.apache.commons.io.FileUtils
import java.io.FilenameFilter
import org.apache.commons.io.filefilter.FileFilterUtils
import scala.collection.JavaConversions._
import org.apache.commons.io.FilenameUtils
import FilenameUtils.EXTENSION_SEPARATOR
import java.io.FileFilter

package object corpus {

  val AnnotationConfFilename = "annotation.conf"

  val CorpusDirFilter = new FileFilter {
    override def accept(f: File) = f.isDirectory() && isCorpusDir(f)
  }

  val AnnotationFilenameFilter = new FilenameFilter {
    override def accept(dir: File, name: String) = {
      val nameParts = name.split(EXTENSION_SEPARATOR)
      nameParts.view(1, nameParts.size).contains("ann")
    }
  }

  def getAnnotationConfFile(annoDoc: AnnoDoc): File = {
    new File(annoDoc.annFile.getParentFile(), AnnotationConfFilename)
  } ensuring (_.isFile())

  def getAnnoDoc(annoFile: File, annotatorId: String): AnnoDoc = {
    val corpusDir = annoFile.getParentFile()
    require(corpusDir != null)
    val (id, errorTag) = {
      annoFile.getName.split(EXTENSION_SEPARATOR) match {
        case Array(id, "ann") => (id, None)
        case Array(id, "ann", errorTag) => (id, Some(errorTag))
        case x => throw new UnsupportedOperationException(
          "Can't parse file name %s".format(annoFile.getName))
      }
    }
    val txtFile = new File(corpusDir, getTxtFilename(id, errorTag))
    require(txtFile.isFile(), s"Text file $txtFile does not exist")
    new AnnoDoc(id, annotatorId, txtFile, annoFile, errorTag)
  }

  def getTxtFilename(doc: AnnoDoc): String = getTxtFilename(doc.id, doc.errorTag)

  def getTxtFilename(id: String, errorTag: Option[String]) = errorTag match {
    case None => "%s.txt".format(id)
    case Some(tag) => "%s.txt.%s".format(id, tag)
  }

  def getAnnFilename(doc: AnnoDoc) = doc.errorTag match {
    case None => "%s.ann".format(doc.id)
    case Some(tag) => "%s.ann.%s".format(doc.id, tag)
  }

  def hasTheSameText(first: File, second: File): Boolean = {
    if (first == second)
      true
    else if (first.lastModified() == second.lastModified())
      true
    else
      FileUtils.contentEquals(first, second)
  }

  def isCorpusDir(dir: File) = new File(dir, AnnotationConfFilename).isFile()
}
