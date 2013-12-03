package ca.scorrent.scapp.Model

import scala.concurrent._
import ExecutionContext.Implicits.global
import ca.curls.test._

/**
 * Created with IntelliJ IDEA.
 * User: CurlyBrackets
 * Date: 11/14/13
 * Time: 4:40 PM
 * To change this template use File | Settings | File Templates.
 */
class Scorrent(val name: String, val tracker: String, val uuid: String) {


  var status: ScorrentState = Waiting

  val totalSize = 209715200

  //TODO: include akka io junk here

  def getPercentDone() = {
    math.random*100
  }

  def changeState(newState: ScorrentState): Future[ScorrentState] = {
    future {
      if(status.isInstanceOf[Error])
        1
        //throw error
      else
        newState match {
          case Waiting =>
            //close connection
            status = Waiting
          case Working =>
            //open connections
            status = Working
          case Error =>
            //close connections if open
            status = Error
        }
        status
    }
  }

  def size(unit: SizeUnit): Int = {
    unit match {
      case MB =>
        totalSize / 1048576
      case KB =>
        totalSize / 1024
      case B =>
        totalSize
    }
  }
}
