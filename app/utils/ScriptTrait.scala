package utils

import play.api.mvc._

trait ScriptTrait extends FileTrait {

  // stdout will be empty if the script fails (run == 1)
  val stdout = new scala.collection.mutable.ArrayBuffer[String]
  // stderr will be empty if the script succeeds (run == 0)
  val stderr = new scala.collection.mutable.ArrayBuffer[String]

  // run == 0 if script succeeds; run==1 if script fails
  def run(body: AnyContent, session: Int): String
}