package model

class Task {
  val name:String
  val id: Int
  
  def update(key:String, value:Any) ={
    
  }
  implicit def reflector(ref: AnyRef) = new {
  def getByName(name: String): Any = ref.getClass.getMethods.find(_.getName == name).get.invoke(ref)
  def setByName(name: String, value: Any): Unit = ref.getClass.getMethods.find(_.getName == name + "_$eq").get.invoke(ref, value.asInstanceOf[AnyRef])
}
  def run() ={}
  def configure(parameters: Map[String, Any])={
    parameters.foreach{ case(k, v) => this.setByName(k, v)}
  }
  
}