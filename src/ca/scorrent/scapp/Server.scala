package ca.scorrent.scapp

/**
 * Created with IntelliJ IDEA.
 * User: ras
 * Date: 27/11/13
 * Time: 8:05 PM
 * To change this template use File | Settings | File Templates.
 */
import akka.actor.{ActorSystem, Actor, Props}
import com.typesafe.config.ConfigFactory
import ca.curls.test.shared.{ChunkRequest, Echo, Chunk}
import java.io.{PrintWriter, BufferedWriter, File}
import com.google.common.io.Files
import java.nio.charset.Charset

/**
 * Server that receives chunks from a client and writes them to disk.
 */
class Server extends Actor {
  var chunks = Vector[Array[Byte]]()
  val LAST_CHUNK = 1 // TODO: Hardcoded for now, will be determined from the .scor file

  def receive = {
    case Chunk(chunkNumber, chunk) =>
      printf("Received chunk # %d: ", chunkNumber)
      chunk.foreach(c => print(c.toChar))
      println()
      chunks = chunks :+ chunk

      if (chunkNumber != LAST_CHUNK) {
        // Get the next chunk from peer
        sender ! ChunkRequest(chunkNumber + 1)
      } else {
        println("Received the last chunk, writing chunks to disk.")
        val writer = new PrintWriter(Files.newWriter(new File("file.txt"), Charset.forName("UTF-8")))
        chunks.foreach(chunk => {
          println("Writing: " + new String(chunk))
          writer.write(new String(chunk))
        })
        writer.close()
      }
    case Echo(msg) =>
      // Start file transfer
      sender ! ChunkRequest(0)
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
  val server = system.actorOf(Props[Server], name = "Server")

  /*val chunkCount = 2
  for (i <- 0 until chunkCount) {
    println("Server requesting chunk # " + i)
    server ! i
  }*/
}

