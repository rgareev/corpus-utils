/**
 *
 */
package ru.kfu.itis.issst.corpus

import java.io.File
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class AnnoDoc(val id: String, val annotatedBy: String,
  val txtFile: File, val annFile: File,
  val errorTag: Option[String] = None) {

  override def toString: String = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
    .append(id).append("annotatedBy", annotatedBy)
    .append("errorTag", errorTag)
    .toString()

}
