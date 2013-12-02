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
import ca.curls.test.shared.Command
import java.io.{PrintWriter, BufferedWriter, File}
import com.google.common.io.Files
import java.nio.charset.Charset

/**
 * Server that receives chunks from a client and writes them to disk.
 */
class Server extends Actor {
  var chunks = Vector[Array[Byte]]()

  def receive = {
    case chunk: Array[Byte] =>
      print("Received a chunk: ")
      chunk.foreach(c => print(c.toChar))
      println()
      chunks = chunks :+ chunk
    case Command("ping") =>
      println("Ping! " + sender.path)
    case Command("done") =>
      println("Client is done sending, writing chunks to disk.")
      val writer = new PrintWriter(Files.newWriter(new File("file.txt"), Charset.forName("UTF-8")))
      chunks.foreach(chunk => {
        println("Writing: " + new String(chunk))
        writer.write(new String(chunk))
      })
      writer.close()
      context stop self
    case _ =>
      println("Unknown item")
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
}

