package utils

package utils

import scala.collection.mutable.Queue

object ScriptScheduler {
  var sessions = Queue[Int]()

  def addSession(session: Int) = {
    sessions += session
    print("added session" + sessions)
  }

  def canRunSession(session: Int): Boolean = {
    if (session == sessions.front) {
      return true
    }
    return false
  }

  def finishedSession(session: Int) {
    if (session == sessions.front) {
      print("finished session " + sessions)
      sessions.dequeue()
    }
  }
}