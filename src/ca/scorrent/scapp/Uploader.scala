package ca.scorrent.scapp

/**
 * Created with IntelliJ IDEA.
 * User: ras
 * Date: 27/11/13
 * Time: 8:02 PM
 * To change this template use File | Settings | File Templates.
 *
 * This class is kept for testing purposes only.
 */
import akka.actor._
import akka.actor.{Props, ActorSystem, Actor}
import com.typesafe.config.ConfigFactory
import java.io.File
import ca.curls.test.shared.{ChunkRequest, Echo, Chunk}
import ca.scorrent.scapp.Services.CheckIn
import scala.concurrent.duration._
import ca.scorrent.scapp.Utils.FileChunker
import akka.io.Tcp.Close

/**
 * Companion object that allows us to pass an argument to Client's constructor.
 */
object Uploader {

  def props(chunks: Vector[Array[Byte]]) =
    Props(classOf[Uploader], chunks)
}

class Uploader(chunks: Vector[Array[Byte]]) extends Actor with ActorLogging {

  val server = context.actorSelection("akka.tcp://HelloRemoteSystem@localhost:1337/user/Server")
  val tracker = context.actorSelection("akka.tcp://TrackerSystem@localhost:1338/user/Tracker")

  def receive: Actor.Receive = {
    case "start" =>
      // This lets server know it can start requesting chunks
      server ! "start"
    case ChunkRequest(chunkNumber) =>
      server ! Chunk(chunkNumber, chunks(chunkNumber))
    case "checkin" =>
      // This tells the peer to checkin with the Tracker
      println("Sending 'checkin' to the tracker")
      tracker ! CheckIn
    case "close" =>
      server ! Close
  }
}

object Main extends App {

  implicit val system = ActorSystem("HelloLocalSystem", ConfigFactory.parseString(
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

  //val filePath: String = Console.readLine("Enter full path to a file to upload: ")
  val chunks = FileChunker.getChunks(new File("/home/ras/ScorrentScapp/file.txt"))

  val client = system.actorOf(Uploader.props(chunks), "Client")
  client ! "start"

  var input: String = _
  do {
    input = Console.readLine("Enter command: ")
    //client ! input
  } while (input != "close")

  import system.dispatcher

  //system.scheduler.schedule(FiniteDuration(0L, SECONDS), FiniteDuration(15L, SECONDS), client, "checkin")
}
