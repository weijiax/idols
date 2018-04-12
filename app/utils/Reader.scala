package utils

import java.nio.file._

object Reader {

  /**
   * Convert the content of a file to HTML
   */
  def toHTML(format: String, src: String): String = {

    format match {
      case "markdown" => { return com.github.rjeschke.txtmark.Processor.process(src) }
      case "text" | "html" => { return src }
    }
  }

}