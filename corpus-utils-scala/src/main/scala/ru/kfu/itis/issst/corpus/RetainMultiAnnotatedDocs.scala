/**
 *
 */
package ru.kfu.itis.issst.corpus

import java.io.File
import scopt.OptionParser
import com.typesafe.scalalogging.Logger
import com.typesafe.scalalogging.slf4j.StrictLogging
import scala.collection.{ mutable => mu }
import org.apache.commons.io.filefilter.FileFilterUtils
import java.io.FilenameFilter
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.FileUtils

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
object RetainMultiAnnotatedDocs extends StrictLogging {

  val AnnotationConfFilename = "annotation.conf"
  val DocAnnotationFileSuffix = ".ann"
  val DocAnnotationFilefilter: FilenameFilter = FileFilterUtils.suffixFileFilter(DocAnnotationFileSuffix)

  private case class Config(dataDir: File = null, outputDir: File = null)

  def main(args: Array[String]) {
    val cmdParser = new OptionParser[Config]("retain-multi-annotated-docs") {
      opt[File]('d', "data-dir") required () valueName ("<dir>") action (
        (arg, cfg) => cfg.copy(dataDir = arg)) validate (arg =>
          if (arg.isDirectory()) success else failure(s"$arg is not an existing dir"))
      opt[File]('o', "output-dir") required () valueName ("<dir>") action (
        (arg, cfg) => cfg.copy(outputDir = arg))
    }
    cmdParser.parse(args, Config()) match {
      case Some(cfg) => run(cfg)
      case None => sys.exit(1)
    }
  }

  private def run(cfg: Config) {
    val corpusDirs = cfg.dataDir.listFiles().filter(isCorpusDir(_))
    logger.info("Corpus dirs: {}", corpusDirs.mkString(","))

    val docCounts = mu.Map.empty[String, Int]
    def countDocs(corpusDir: File) {
      for (docId <- getDocIds(corpusDir))
        docCounts.get(docId) match {
          case Some(x) => docCounts(docId) = x + 1
          case None => docCounts(docId) = 1
        }
    }
    corpusDirs.foreach(countDocs(_))

    def processCorpus(corpusDir: File, outputDir: File) {
      for (docId <- getDocIds(corpusDir))
        if (docCounts(docId) > 1) {
          copyDoc(docId, corpusDir, outputDir)
          logger.info("Copied doc {}", docId)
        } else {
          logger.info("Doc {} is ignored", docId)
        }
    }
    for (cDir <- corpusDirs)
      processCorpus(cDir, new File(cfg.outputDir, cDir.getName))
  }

  private def isCorpusDir(dir: File) = new File(dir, AnnotationConfFilename).isFile()

  private def getDocIds(dir: File) =
    for {
      candFilename <- dir.list(DocAnnotationFilefilter)
      candId = toDocId(candFilename)
      if (new File(dir, toDocTextFilename(candId)).isFile())
    } yield candId

  private def copyDoc(docId: String, from: File, to: File) {
    val docTextFilename = toDocTextFilename(docId)
    val docAnnFilename = toDocAnnFilename(docId)
    FileUtils.copyFile(new File(from, docTextFilename), new File(to, docTextFilename))
    FileUtils.copyFile(new File(from, docAnnFilename), new File(to, docAnnFilename))
  }

  private def toDocTextFilename(docId: String) = docId + ".txt"

  private def toDocAnnFilename(docId: String) = docId + DocAnnotationFileSuffix

  private def toDocId(docAnnFilename: String) = FilenameUtils.getBaseName(docAnnFilename)
}