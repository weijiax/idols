
package models.tasks

import play.api.mvc._
import models.tasks.helperFunctions._
import scala.sys.process._
import scala.collection.Seq
import play.api.libs.json._

class streamTweetsTask(json: JsValue) extends Task(json) {
  //  var file : File
  //  var target : String
  //
  def run(body: AnyContent, session: Int): String = {
    collectTweets(body)
  }

  /**
   * check Cluster info, node list, etc.
   */
  def collectTweets(body: AnyContent): String = {
    var feedback = ""

    val keywords = body.asFormUrlEncoded.get("keywords")(0).replace("\"", "\\\"")
    val stream_time = body.asFormUrlEncoded.get("stream_time")(0)

    println(keywords)

    //val reservationName ="hadoop+Idols+2431"

    val stream_tweets_script_dir = body.asFormUrlEncoded.get("script_dir")(0)

    val command = "cd " + stream_tweets_script_dir + "; source run_streaming_keywords.sh" + " " + keywords + " " + stream_time

    println(command)

    //val res = 0
    val res = Process(Seq("bash", "-c", command)).!

    println(res)

    val command_2 = "cd " + stream_tweets_script_dir + "; cat log/tweets.log | wc -l"

    println(command_2)

    val num_line = Process(Seq("bash", "-c", command_2)).!!

    println(num_line)

    val currentDirectory = new java.io.File(".").getCanonicalPath

    val image_path = currentDirectory + "/public/images/tweets_map.png"

    println(image_path)

    val command_1 = "module load Rstats/3.4.0; cd " + stream_tweets_script_dir + "; Rscript process_tweets_log.R ./log/tweets.log " + image_path
    //    val command_1 = "cd " + stream_tweets_script_dir + "; Rscript process_tweets_log.R ./log/tweets.log " + image_path

    println(command_1)

    Process(Seq("bash", "-c", command_1)).!

    res match {
      case 0 => { feedback = "Collected " + num_line + "tweets" }
      case _ => { feedback = "Failed: something went wrong" }
    }

    feedback

  }

  // description,
  //tag is div, p, or others to form <div></div>

}