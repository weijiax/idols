package models

import scala.sys.process._
import org.htmlcleaner.HtmlCleaner
import java.net.URL
import java.net.URLDecoder



object helperFunctions {
  
    // description, 
    //tag is div, p, or others to form <div></div>
    def arrayToHtml(description: String = "Reservation information", stringArray:Array[String], tag:String = "div"): String = {
      val start = "<" + tag + ">"
      val end = "</" + tag + ">"
      val desc_html = start + description + end
      stringArray.length match {
        case 1 => { desc_html + start + stringArray(0) + end}
        case _ => { desc_html + start + stringArray.mkString(start+end) + end}
      }
    }
    
      // get first node name e.g c252-101 in the reservation
      def getFirstNode(resName:String="hadoop-test"):String = {
        val command = "tmp=`sinfo -T|grep "+ resName + " |awk '{print $6}'` && scontrol show hostname $tmp | head -n 1"
        Process(Seq("bash","-c", command)).!!.split("\n")(0)
      }
      
      // list jobs, return array of apps
      def listHapdoopJob(status:String = "ALL") = {
        var command = ""
        status match {
          case "ALL" => command = "yarn application -list -appStates " +  status + " | grep $USER | awk '{print $1}'"
          case _     => command = "yarn application -list -appStates ALL" + " | grep " + status + " | grep $USER | awk '{print $1}'"
        }
        
        Process(Seq("bash","-c", command)).!!.split("\n")    
      }
      
      // get job status URL
      def getJobURL(firstNode:String = "c251-101", app:String = "application_1509916447369_0003"):String = {
        //"http://" + firstNode + ":48088/cluster/app/" + app
        "http://" + firstNode + ":8088/cluster/app/" + app
           
      }
      
      // get job status table body
      // http://wrangler.tacc.utexas.edu:48088/cluster/app/application_1511372443652_0016
      //                 http://c251-101:8088/cluster/app/application_1511372443652_0016
      def getTbody(url:String = "http://wrangler.tacc.utexas.edu:48088/cluster/app/application_1511372443652_0016")= {
          val cleaner = new HtmlCleaner
          val props = cleaner.getProperties
          val rootNode = cleaner.clean(new URL(url))
          val test = rootNode.findElementByAttValue("class", "info", true, true)      
          cleaner.getInnerHtml(test)
      }
    
    
    
}