/**
 *
 */
package ru.kfu.itis.issst.corpus

import com.typesafe.scalalogging.slf4j.StrictLogging
import java.io.File
import scopt.OptionParser
import org.apache.commons.io.FileUtils

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class ReplenishAnnoDocRepo(private val srcRepo: AnnoDocRepo,
  private val targetRepo: AnnoDocRepo,
  private val reviewRepo: AnnoDocRepo,
  private val ignoreDocs: Set[String]) extends StrictLogging {

  def run() {
    for {
      annotatorId <- srcRepo.getAnnotatorIds()
      srcDoc <- srcRepo.getDocsAnnotatedBy(annotatorId)
      if !ignoreDocs.contains(srcDoc.id)
    } {
      if (srcDoc.errorTag.isDefined)
        throw new UnsupportedOperationException(s"Source document $srcDoc has an error tag")
      targetRepo.getDoc(srcDoc.id, annotatorId) match {
        case None =>
          // send to review
          reviewRepo.add(srcDoc)
          logger.info("Document {} is added to review", srcDoc)
        case Some(targetDoc) =>
          if (!hasTheSameText(srcDoc.txtFile, targetDoc.txtFile))
            throw new IllegalStateException(s"Doc $srcDoc has a different text in the target repo")
          targetDoc.errorTag match {
            case None =>
              // no tag for src and target, check annotation updates
              if (hasTheSameText(srcDoc.annFile, targetDoc.annFile)) {
                logger.info("Document {} is not changed", srcDoc)
              } else {
                // keep the newest
                if (srcDoc.annFile.lastModified() > targetDoc.annFile.lastModified()) {
                  targetRepo.update(srcDoc)
                  logger.info("Document {} has been updated", srcDoc)
                } else {
                  logger.info("Target repository has the newer version of {}", srcDoc)
                }
              }
            case Some(errorTag) if Set("big", "bad").contains(errorTag) =>
              if (hasTheSameText(srcDoc.annFile, targetDoc.annFile)) {
                logger.info("Document {} is not changed", srcDoc)
              } else {
                reviewRepo.add(srcDoc)
                logger.info("Document {} is added to review", srcDoc)
              }
            case Some(errorTag) if errorTag.startsWith("err") =>
              logger.warn("Document {} is skipped because of its error tag")
            case Some(errorTag) =>
              throw new UnsupportedOperationException(s"Unknown error tag in $srcDoc")
          }
      }
    }

  }
}

object ReplenishAnnoDocRepo {
  private case class Config(srcBaseDir: File = null, targetBaseDir: File = null,
    reviewBaseDir: File = null, ignoreIdFiles: List[File] = Nil)

  def main(args: Array[String]) {
    val cmdParser = new OptionParser[Config]("replenish-annotated-doc-repository") {
      opt[File]('s', "source-repo") required () valueName ("<dir>") action (
        (arg, cfg) => cfg.copy(srcBaseDir = arg))
      opt[File]('t', "target-repo") required () valueName ("<dir>") action (
        (arg, cfg) => cfg.copy(targetBaseDir = arg))
      opt[File]('r', "review-repo") required () valueName ("<dir>") action (
        (arg, cfg) => cfg.copy(reviewBaseDir = arg))
      opt[File]('i', "ignore-file") minOccurs (0) unbounded () valueName ("<file>") action (
        (arg, cfg) => cfg.copy(ignoreIdFiles = arg :: cfg.ignoreIdFiles))
    }
    cmdParser.parse(args, Config()) match {
      case Some(cfg) =>
        val srcRepo = new SimpleAnnoDocRepo(cfg.srcBaseDir)
        val targetRepo = new SimpleAnnoDocRepo(cfg.targetBaseDir)
        val reviewRepo = new SimpleAnnoDocRepo(cfg.reviewBaseDir)
        import scala.collection.JavaConversions.collectionAsScalaIterable
        val ignoreIds =
          for {
            ignoreListFile <- cfg.ignoreIdFiles
            ignoreId <- FileUtils.readLines(ignoreListFile, "utf-8")
          } yield ignoreId.trim()
        new ReplenishAnnoDocRepo(srcRepo, targetRepo, reviewRepo, ignoreIds.toSet).run()
      case None => sys.exit(1)
    }
  }
}