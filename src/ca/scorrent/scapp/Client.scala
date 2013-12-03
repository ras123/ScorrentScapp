package ca.scorrent.scapp

/**
 * Created with IntelliJ IDEA.
 * User: ras
 * Date: 27/11/13
 * Time: 8:02 PM
 * To change this template use File | Settings | File Templates.
 */
import akka.actor._
import akka.actor.{Props, ActorSystem, Actor}
import com.typesafe.config.ConfigFactory
import java.io.File
import ca.curls.test.shared.{ChunkRequest, Echo, Chunk}
import ca.scorrent.scapp.Services.CheckIn
import scala.concurrent.duration._

/**
 * Reads a file and splits it into fixed-size chunks. Returns the chunks in sequential order
 * using the method next().
 */
class FileChunker {
  var idx = 0
  val CHUNK_LENGTH = 10
  var chunks = Vector[Array[Byte]]()

  /**
   * Reads a file and splits it into fixed-size chunks.
   * @param filePath  absolute path to the file
   */
  def read(filePath: String) {
    val plzChunkMe = com.google.common.io.Files toByteArray new File(filePath)
    println(s"Size of $filePath: " + plzChunkMe.length)

    val numChunks =
      if (plzChunkMe.length % CHUNK_LENGTH == 0)
        plzChunkMe.length / CHUNK_LENGTH
      else
        plzChunkMe.length / CHUNK_LENGTH + 1

    for (i <- 0 to numChunks - 1) {
      val baseIndex = i * CHUNK_LENGTH
      val chunk = plzChunkMe slice(baseIndex, baseIndex + CHUNK_LENGTH)
      chunks = chunks :+ chunk
    }
  }

  /**
   * Returns the next file chunk if there is one and increments the index.
   * @return  next file chunk wrapped in Option
   */
  def next(): Option[Array[Byte]] = {
    var ret: Option[Array[Byte]] = None
    if (idx < chunks.length) {
      ret = Some(chunks(idx))
      idx += 1
    }

    ret
  }
}

/**
 * Companion object that allows us to pass an argument to Client's constructor.
 */
object Client {

  def props(fileChunker: FileChunker) =
    Props(classOf[Client], fileChunker)
}

class Client(fileChunker: FileChunker) extends Actor with ActorLogging {

  val server = context.actorSelection("akka.tcp://HelloRemoteSystem@localhost:1337/user/Server")
  val tracker = context.actorSelection("akka.tcp://TrackerSystem@localhost:1338/user/Tracker")

  def receive: Actor.Receive = {
    case "start" =>
      // This lets server know it can start requesting chunks
      server ! Echo("blah")
      /*fileChunker.next() match {
        case Some(chunk) =>
          server ! chunk
      }*/
    case ChunkRequest(chunkNumber) =>
      server ! Chunk(chunkNumber, fileChunker.chunks(chunkNumber))
    case "checkin" =>
      // This tells the peer to checkin with the Tracker
      tracker ! CheckIn
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

  var input: String = Console.readLine("Enter full path to a file to upload: ")
  val fileChunker = new FileChunker
  fileChunker.read(input)

  val client = system.actorOf(Client.props(fileChunker))
  client ! "start"

  import system.dispatcher

  system.scheduler.schedule(FiniteDuration(0L, SECONDS), FiniteDuration(15L, SECONDS), client, "checkin")

  // Type 'send' commands to send a chunk to server
  /*do {
    input = Console.readLine("Client> ")
    if (input != "stop")
      client ! Command(input)
  } while (input != "stop")

  system.shutdown()
  println("Connection closed")*/
}
