package ca.scorrent.scapp.Services

import akka.actor.{Props, ActorSystem, ActorRef, Actor}
import scala.collection.immutable.HashMap
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

/**
 * Created with IntelliJ IDEA.
 * User: CurlyBrackets
 * Date: 12/2/13
 * Time: 4:15 PM
 * To change this template use File | Settings | File Templates.
 */
class Tracker extends Actor{
  var peers: Map[ActorRef, Long] = new HashMap[ActorRef, Long]()
  var oldTimes: Map[ActorRef, Long] = new HashMap[ActorRef, Long]()

  def receive = {
    case "Stop" =>
      context stop self
    case CheckIn =>
      println("Received a checkin from: " + sender.path.toString())
      if (peers contains sender) {
        oldTimes = oldTimes + (sender -> peers(sender))
      }
      peers = peers.filterNot(
        (pair) => {
          pair._1 == sender
        }
      ) + (sender -> System.currentTimeMillis)
      sender ! Success
    case PeerUpdate =>
      val oTime = oldTimes(sender)
      sender ! NewPeers((peers.filter(
                            (pair) => {
                              pair._2 > oTime
                            }
                          ).size) != 0)
    case PeerRequest =>
      // Return all peers in the swarm not including the sender
      val swarm = peers.keys.toList
      sender ! Peers(for (p <- swarm if p != sender) yield p.path)
    case Prune =>
      println("Pruning..")
      peers = peers.filter(
        (pair) => {
          pair._2 > System.currentTimeMillis()-(HeartBeat.Rate*2000)
        }
      )
    case _ =>
      println("Fuck off wrong thing sent")
  }
}

object TrackerDriver extends App {
  val system = ActorSystem("TrackerSystem", ConfigFactory.parseString("""
    akka {
       actor {
           provider = "akka.remote.RemoteActorRefProvider"
       }
       remote {
           transport = "akka.remote.netty.NettyRemoteTransport"
           netty.tcp {
              hostname = "localhost"
              port = 1338
           }
        }
    }
  """))

  val tracker = system.actorOf(Props[Tracker], name = "Tracker")

  import system.dispatcher
  system.scheduler.schedule(0 milliseconds, HeartBeat.Rate * 2 seconds, tracker, Prune)
}
