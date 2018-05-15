package utils

import java.nio.file._

object Reader {

  /**
   * Convert source string to HTML
   * @param format: text, html, or markdown
   * @param src: source string to be converted
   * @return converted html string
   */
  def toHTML(format: String, src: String): String = {
    format match {
      case "markdown" => { return com.github.rjeschke.txtmark.Processor.process(src) }
      case "text" | "html" => { return src }
    }
  }
}