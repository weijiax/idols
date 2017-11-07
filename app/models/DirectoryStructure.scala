package models

import java.io.File
import scala.collection.mutable.ListBuffer

case class DirectoryStructure(rootPath: String) {
  
  var result: String = ""
  
  // build directory tree with root node
  var root: Node = new Node(new File(rootPath), s"$rootPath")
  root = buildTree(root, 1)
  
  // traverse tree to build json string
  var jsonString: StringBuffer = new StringBuffer
  jsonString.append("[")
  jsonString = buildJSON(root, jsonString)
  // delete the last comma at the end of string
  jsonString.setLength(jsonString.length() - 1)
  jsonString.append("]")
  result = jsonString.toString()

  /**
   * @param f: current file
   * @param n: relative path of the file
   */
  case class Node(f: File, n: String) {
    val file = f
    val name = n
    var children: ListBuffer[Node] = ListBuffer[Node]()

    def addChild(child: Node): Unit = children += child
    def getChildren: Seq[Node] = children.toSeq

  }
  
  // return json string 
  def getJsonString(): String = {
    return result
  }
  
  /**
   *  use recursion to build a directry tree by going through all the sub-directories
   *  @param node: the current node
   *  @return the root node with the directory tree structure
   */
  def buildTree(node: Node, depth: Integer): Node = {
    val children: Array[File] = node.file.listFiles()
    if (children == null || depth == 3)
      // leaf node, return node
      return node
    else {
      var i = 0;
      while (i < children.size) {
        if (!children(i).isHidden() && children(i).isDirectory()) {
          // keep un-hidden directories only
          // the name of the node is its relative path
          var childName = children(i).getAbsolutePath.substring(node.file.getAbsolutePath.length() + 1)
          val n = new Node(children(i), childName)
          node.addChild(n)
          buildTree(n, depth + 1)
        }
        i = i + 1
      }
      return node
    }
  }
  
  /**
   *  pre-order traversal
   *  for testing only
   *  print out the directory structure
   */
  def printTree(node: Node, space: String) {
    if (node == null)
      return 
    println(space + node.name)
    val children: Seq[Node] = node.getChildren
    children.foreach(child => printTree(child, space + "\t"))
  }

  /**
   * build json string by pre-order traversing the directory tree
   * @param node: the current node
   * @param result: the json string in StringBuffer
   * @return the result json string StringBuffer
   */
  def buildJSON(node: Node, result: StringBuffer): StringBuffer = {
    if (node == null)
      return result
    // add info for the node to result StringBuffer
    result.append("{")
    result.append("\"text\":\"" + node.name + "\",")
    result.append("\"data\":\"" + node.f.getAbsolutePath + "\",")
    // folder will be opened automatically
    result.append("\"state\" : {\"opened\" : true },")
    
    // loop through the children File
    val children: Seq[Node] = node.getChildren
    if (children.length > 0) {
      result.append("\"children\":[")
      var index: Int = 0
      while (index < children.length) {
        buildJSON(children(index), result)
        if (index == children.length - 1)
          // last children, delete the ',' at the end
          result.setLength(result.length() - 1)
        index = index + 1
      }
      result.append("]")
    } else {
      result.setLength(result.length() - 1)
    }
    result.append("},")
  }
}
