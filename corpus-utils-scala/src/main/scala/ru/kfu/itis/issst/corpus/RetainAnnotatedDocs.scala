/**
 *
 */
package ru.kfu.itis.issst.corpus

import java.io.File
import scopt.OptionParser
import com.typesafe.scalalogging.Logger
import com.typesafe.scalalogging.slf4j.StrictLogging

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
object RetainAnnotatedDocs extends StrictLogging {

  val AnnotationConfFilename = "annotation.conf"

  private[RetainAnnotatedDocs] case class Config(dataDir: File = null, outputDir: File = null)

  def main(args: Array[String]) {
    val cmdParser = new OptionParser[Config]("retain-annotated-docs") {
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
    for(cDir <- corpusDirs) 
    	processCorpus(cDir, new File(cfg.outputDir, cDir.getName))
  }

  private def isCorpusDir(dir: File) = new File(dir, AnnotationConfFilename).isFile()
  
  private def processCorpus(corpusDir:File, outputDir:File) {
    // TODO corpusDir.s
  }
}