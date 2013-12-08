package ca.scorrent.scapp.Services

import ca.scorrent.scapp.Model.Waiting
import akka.actor._
import ca.scorrent.scapp.Model.Seeding
import ca.scorrent.scapp.Model.Downloading
import ca.scorrent.scapp.Utils.{FileChunker, ChunkWriter}
import ca.scorrent.scapp.UI.DownloadDirectory
import java.io.File
import ca.scorrent.scapp.UI.UserPrefs
import scala.collection.mutable.ListBuffer
import ca.scorrent.scapp.UI.ScorrentView
import akka.io.Tcp.Close
import ca.curls.test.shared.ChunkRequest
import akka.actor.Terminated
import ca.curls.test.shared.Chunk
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

/**
 * Created with IntelliJ IDEA.
 * User: ras
 * Date: 07/12/13
 * Time: 5:16 PM
 * To change this template use File | Settings | File Templates.
 *
 * This file contains upload and download functionality.
 */

object Peer {
  object PeerDownload {
    def props(scView: ScorrentView) =
      Props(classOf[PeerDownload], scView)
  }

  class PeerDownload(scView: ScorrentView) extends Actor {
    println("PeerDownload: downloading " + scView.scorrent.numOfChunks + " chunks.")
    val sc = scView.scorrent
    var corruptedChunks = new ListBuffer[Int]
    val chunksDownloaded = sc.numOfChunks - sc.getMissingChunks()._1
    println("Chunks missing: " + sc.getMissingChunks()._1)

    // Create a ChunkWriter for storing received chunks
    val fileName = scView.scorrent.files(0)
    val filePath = UserPrefs.get[File](DownloadDirectory).getAbsolutePath + File.separator + fileName
    var writer = new ChunkWriter(new File(filePath), chunksDownloaded)

    val tracker = context.actorSelection(sc.tracker)
    scView.progressBar.value = ScorrentView.PROGRESS_BAR_MAX * chunksDownloaded / sc.numOfChunks

    def receive = {
      case "start" =>
        // Register in the swarm
        tracker ! Register(sc.uuid)
        tracker ! PeerRequest(sc.uuid)
      case "getpeers" =>
        println("Asking tracker for peers")
        // Request a list of peers from the tracker
        tracker ! PeerRequest(sc.uuid)
      case Peers(peers) =>
        if (peers.length != 0 && sc.peers.size == 0) {
          println("Received Peers message: " + peers)
          // TODO: For now just select the first peer and start receiving chunks from it
          val peer = context.actorSelection(peers.head)
          peer ! "handshake"
        }
      case "handshake" =>
        println("Received handshake from: " + sender.path)
        scView.setMode(Downloading)
        // Watch for peer termination
        context.watch(sender)
        sc.registerPeers(List(sender.path))
        // Start requesting chunks
        sender ! ChunkRequest(sc.nextChunkIdx)
      case Chunk(chunkNumber, chunk) =>
        printf("Received chunk # %d\n", chunkNumber)

        if (writer.verifyChunk(chunk, sc.chunkHashes(chunkNumber))) {
          writer.writeChunk(chunk, chunkNumber)
          if (writer.chunksWritten % 5 == 0) {
            scView.progressBar.value = ScorrentView.PROGRESS_BAR_MAX * writer.chunksWritten / sc.numOfChunks
          }
        } else {
          // TODO: We should retry or something
          println("Hash of the received chunk does not match!")
          corruptedChunks += chunkNumber
        }

        val chunkIdx = sc.nextChunkIdx
        if (chunkIdx != -1) {
          // Get the next chunk from peer
          sender ! ChunkRequest(chunkIdx)
        } else if (corruptedChunks.isEmpty) {
          // Successfully received all file chunks
          println("Download finished!")
          scView.setMode(Seeding)
          scView.progressBar.value = ScorrentView.PROGRESS_BAR_MAX
          sender ! Close
        }
      case "checkin" =>
        // Ping the tracker to let it know we're alive
        tracker ! CheckIn(sc.uuid)
      case Terminated(peer) =>
        println("Peer terminated connection: " + peer)
        // Unregister peer that closed the connection
        sc.unregisterPeer(peer.path)
        if (sc.peers.size == 0) {
          println("No more active peers")
          scView.setMode(Waiting)
          self ! "getpeers"
        }
    }
  }

  def startDownloading(scView: ScorrentView) = {
    println("Starting downloading")
    implicit val system = ActorSystem("Peer", ConfigFactory.parseString(
      """
akka {
  actor {
    provider = "akka.remote.RemoteActorRefProvider"
  }
  remote {
     transport = "akka.remote.netty.NettyRemoteTransport"
     netty.tcp {
       hostname = "localhost"
       port = 0
     }
   }
}
      """))

    val peer = system.actorOf(PeerDownload.props(scView), name = "Peer")
    system.scheduler.schedule(FiniteDuration(HeartBeat.Rate, SECONDS), FiniteDuration(HeartBeat.Rate, SECONDS), peer, "checkin")
    system.scheduler.schedule(FiniteDuration(HeartBeat.Rate, SECONDS), FiniteDuration(HeartBeat.Rate, SECONDS), peer, "getpeers")

    peer ! "start"
  }

  object PeerUpload {
    def props(scView: ScorrentView, chunks: Vector[Array[Byte]]) =
      Props(classOf[PeerUpload], scView, chunks)
  }

  class PeerUpload(scView: ScorrentView, chunks: Vector[Array[Byte]]) extends Actor with ActorLogging {
    val sc = scView.scorrent
    val tracker = context.actorSelection(sc.tracker)

    def receive: Actor.Receive = {
      case "start" =>
        // Register in the swarm
        tracker ! Register(sc.uuid)
      case ChunkRequest(chunkNumber) =>
        sender ! Chunk(chunkNumber, chunks(chunkNumber))
      case "handshake" =>
        // This starts up the connection between downloader & uploader
        sender ! "handshake"
      case "checkin" =>
        // This tells the peer to checkin with the Tracker
        tracker ! CheckIn(sc.uuid)
    }
  }

  def startSeeding(scView: ScorrentView) = {
    println("Starting uploading")
    implicit val system = ActorSystem("Peer", ConfigFactory.parseString(
      """
akka {
  actor {
    provider = "akka.remote.RemoteActorRefProvider"
  }
  remote {
     transport = "akka.remote.netty.NettyRemoteTransport"
     netty.tcp {
       hostname = "localhost"
       port = 0
     }
   }
}
      """))

    // Open the file to be seeded and split it into chunks
    val fileName = scView.scorrent.files(0)
    var filePath = UserPrefs.get[File](DownloadDirectory).getAbsolutePath + File.separator + fileName
    val file = new File(filePath)
    val seed = system.actorOf(PeerUpload.props(scView, FileChunker.getChunks(file)), "Seed")

    system.scheduler.schedule(FiniteDuration(HeartBeat.Rate, SECONDS), FiniteDuration(HeartBeat.Rate, SECONDS), seed, "checkin")

    seed ! "start"
  }
}