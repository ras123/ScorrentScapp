package ca.scorrent.scapp.Model

import scala.concurrent._
import ExecutionContext.Implicits.global
import ca.curls.test._
import akka.actor._
import ca.curls.test.shared.{Chunk, ChunkRequest}
import ca.scorrent.scapp.Services.CheckIn
import scala.util.Random
import scala.collection.mutable.HashSet

/**
 * Created with IntelliJ IDEA.
 * User: CurlyBrackets
 * Date: 11/14/13
 * Time: 4:40 PM
 * To change this template use File | Settings | File Templates.
 */
class Scorrent(val name: String,
               val tracker: String,
               val uuid: String,
               val numOfChunks: Integer,
               val files: Vector[String],
               val chunkHashes: Vector[String],
               var chunkIndices: List[Int] = Nil,
               var status: ScorrentState) {
  var peers = new HashSet[ActorPath]

  if (status != Seeding && chunkIndices == Nil) {
    println("Generating random list of chunk indices")
    chunkIndices = Random.shuffle((0 until numOfChunks).toList)
  }

  def nextChunkIdx(): Int = {
    var chunkIdx = -1
    if (!chunkIndices.isEmpty) {
      chunkIdx = chunkIndices.head
      chunkIndices = chunkIndices.tail
    }

    chunkIdx
  }

  /*
   * Returns as a pair the number of chunks missing and their String representation
   * separated by ',' characters.
   */
  def getMissingChunks(): Pair[Int, String] = {
    var chunksMissingCount = 0
    var chunksMissing = new StringBuilder()
    chunkIndices.foreach(idx => {
      chunksMissingCount += 1
      chunksMissing.append(idx.toString + ",")
    })

    (chunksMissingCount, chunksMissing.toString())
  }

  def registerPeers(newPeers: List[ActorPath]) {
    newPeers.foreach(peer => {
      if (!peers.contains(peer)) {
        println("Registering: " + peer)
        peers.add(peer)
      }
    })
  }

  def unregisterPeer(peer: ActorPath) {
    if (peers.contains(peer)) {
      println("Unregistering: " + peer)
      peers.remove(peer)
    }
  }

  def getPercentDone() = {
    math.random * 100
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
          case Downloading =>
            status = Downloading
          case Seeding =>
            status = Seeding
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
        200
      case KB =>
        200
      case B =>
        200
    }
  }
}
