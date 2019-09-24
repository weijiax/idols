package models

import java.io.File
import scala.collection.mutable.ListBuffer
import play.api.libs.json._
import sys.process._

case class DirectoryStructure(rootPath: String) {

  var result: String = ""

  val rootString = Process(Seq("bash", "-c", "echo " + rootPath)).!!.split("\n")(0)

  // build directory tree with root node
  val f = new File(rootString)
  var root: Node = new Node(f, s"$rootString", 1, !f.isDirectory())
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
  case class Node(f: File, n: String, d: Integer, isF: Boolean) {
    val file = f
    val name = n
    val depth = d
    val isFile = isF
    var children: ListBuffer[Node] = ListBuffer[Node]()

    def addChild(child: Node): Unit = children += child
    def getChildren: Seq[Node] = children.toSeq

  }

  /**
   * @return directory tree as a JsValue
   */
  def getJsValue(): JsValue = {
    return Json.parse(result)
  }

  /**
   * Find a node with target as its path
   * @param n: current node
   * @param target: target path
   * @return node if found, null if not
   */
  def findNode(n: Node, target: String): Node = {
    if (n.file.getAbsolutePath.equals(target))
      return n
    else {
      for (child <- n.children) {
        var current = findNode(child, target)
        if (current != null)
          return current
      }
    }
    return null
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
        if (!children(i).isHidden()) {
          //        if (!children(i).isHidden() && children(i).isDirectory()) {
          // keep un-hidden directories only
          // the name of the node is its relative path
          var childName = children(i).getAbsolutePath.substring(node.file.getAbsolutePath.length() + 1)
          val n = new Node(children(i), childName, depth, !children(i).isDirectory())
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
    if (node.isFile) {
      result.append("\"type\":\"file\",")
    }
    //    // folder will be opened automatically
    //    result.append("\"state\" : {\"opened\" : true")
    //    // default: the root is selected
    //    if (node.name.equals(root.name))
    //      result.append(", \"selected\" : true")
    //    result.append(" },")

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
