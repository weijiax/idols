package utils

trait ScriptTrait extends FileTrait {

  // stdout will be empty if the script fails (run == 1)
  val stdout = new StringBuilder
  // stderr will be empty if the script succeeds (run == 0)
  val stderr = new StringBuilder

  // run == 0 if script succeeds; run==1 if script fails
  def run: Int
}