package ca.scorrent.scapp

/**
 * Created with IntelliJ IDEA.
 * User: ras
 * Date: 27/11/13
 * Time: 8:05 PM
 * To change this template use File | Settings | File Templates.
 *
 * This class is kept for testing purposes only.
 */
import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.io.Tcp.Close
import ca.curls.test.shared.ChunkRequest
import akka.actor.Terminated
import ca.curls.test.shared.Chunk

/**
 * Server that receives chunks from a client and writes them to disk.
 */
class Downloader extends Actor {
  var chunks = Vector[Array[Byte]]()
  val LAST_CHUNK = 49 // TODO: Hardcoded for now, will be determined from the .scor file


  def receive = {
    case Chunk(chunkNumber, chunk) =>
      printf("Received chunk # %d: ", chunkNumber)
      chunk.foreach(c => print(c.toChar))
      println()
      chunks = chunks :+ chunk

      Thread.sleep(500)
      if (chunkNumber != LAST_CHUNK) {
        // Get the next chunk from peer
        sender ! ChunkRequest(chunkNumber + 1)
      } else {
        println("Received the last chunk, writing chunks to disk.")
        /*val writer = new PrintWriter(Files.newWriter(new File("file.txt"), Charset.forName("UTF-8")))
        chunks.foreach(chunk => {
          println("Writing: " + new String(chunk))
          writer.write(new String(chunk))
        })
        writer.close()*/
      }
    case "start" =>
      context.watch(sender)
      // Start file transfer
      sender ! ChunkRequest(0)
    case Terminated(actor) =>
      println("Peer terminated connection: " + actor)
      println("Peer path: " + actor.path)
    case Close => println("Close")
  }
}

object ServerDriver extends App {

  val system = ActorSystem("HelloRemoteSystem", ConfigFactory.parseString("""
    akka {
       actor {
           provider = "akka.remote.RemoteActorRefProvider"
       }
       remote {
           transport = "akka.remote.netty.NettyRemoteTransport"
           netty.tcp {
              hostname = "localhost"
              port = 1337
           }
        }
    }
  """))
  val server = system.actorOf(Props[Downloader], name = "Server")

  system.shutdown()
}

