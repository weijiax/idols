package models

/**
 * Presentation object used for displaying data in a template.
 *
 * Note that it's a good practice to keep the presentation DTO,
 * which are used for reads, distinct from the form processing DTO,
 * which are used for writes.
 */
import play.api.libs.Files
import play.api.mvc._


abstract class Cluster(numNode: Int, numDays:Int, cType: String) {
  //number of node for the cluster
  val numberNode: Int
  
  // number of days for the cluster
  val numberDays: Int
  
  // type of this task, example: fileUpload
  val clusterType: String
  
//  val id: Int
  
  /**
   * Run this task
   * @param body: message requested from user
   * @return feedback to user
   */
  def create()


  implicit def reflector(ref: AnyRef) = new {
    def getByName(name: String): Any = ref.getClass.getMethods.find(_.getName == name).get.invoke(ref)
    def setByName(name: String, value: Any): Unit = ref.getClass.getMethods.find(_.getName == name + "_$eq").get.invoke(ref, value.asInstanceOf[AnyRef])
  }
 
  def configure(parameters: Map[String, Any]) = {
    parameters.foreach { case (k, v) => this.setByName(k, v) }
  }
  
  def getNodeList(res: String): Array[String]
  
  

}