package ca.scorrent.scapp.Services

import akka.actor.{ActorRef, ActorSystem, Actor, Props}
import com.typesafe.config.ConfigFactory
import akka.remote._
import akka.remote.RemoteActorRefProvider
import akka.actor.{ActorSystem, Actor, Props}
import com.typesafe.config.ConfigFactory
import ca.curls.test.shared.{ChunkRequest, Echo, Chunk}
import java.io.{PrintWriter, BufferedWriter, File}
import com.google.common.io.Files
import java.nio.charset.Charset


/**
 * Created with IntelliJ IDEA.
 * User: CurlyBrackets
 * Date: 12/2/13
 * Time: 7:01 PM
 * To change this template use File | Settings | File Templates.
 */
object TrackerServer extends App{
  val system = ActorSystem("HelloRemoteSystem", ConfigFactory.parseString("""
    akka {
       actor {
           provider = "akka.remote.RemoteActorRefProvider"
       }
       remote {
           transport = "akka.remote.netty.NettyRemoteTransport"
           netty.tcp {
              hostname = "localhost"
              port = 6969
           }
        }
    }
  """))
  println("Hi")
  val manager = system.actorOf(Props(new Manager(system)), name="Manager")

  var input:String = ""
  do {
    input = Console.readLine("")
  } while (input != "stop")
  manager ! "Stop"
  system.shutdown()
  println("Connection closed")
}

//object Manager

class Manager(val system: ActorSystem) extends Actor{
  var ScorTrackers = List[ActorRef]()

  def receive = {
    case "Stop" =>
      ScorTrackers.foreach((ar) => {ar ! "Stop"})
      //fuck saving things
      context stop self
    case Register(uuid: String) =>
      ScorTrackers = ScorTrackers :+ system.actorOf(Props[Tracker], name = uuid)
  }
}