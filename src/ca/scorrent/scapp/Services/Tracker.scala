package ca.scorrent.scapp.Services

import akka.actor.{Props, ActorSystem, ActorRef, Actor}
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
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
  var peers: Map[String, List[Pair[ActorRef, Long]]] = new HashMap[String, List[Pair[ActorRef, Long]]]()

  def receive = {
    case Register(uuid) =>
      var swarm: List[Pair[ActorRef, Long]] = Nil
      if (peers.contains(uuid)) {
        swarm = peers(uuid)
      }
      peers += (uuid -> ((sender, System.currentTimeMillis()) :: swarm))
      println(s"Registering: ${uuid} -> ${sender}")
    case PeerRequest(uuid) =>
      // Return all peers in the swarm not including the sender
      if (peers.contains(uuid)) {
        val swarm = peers(uuid)
        sender ! Peers(for (peer <- swarm if peer._1 != sender) yield peer._1.path)
      } else {
        sender ! Peers(List())
      }
    case CheckIn(uuid) =>
      println(s"Received a checkin from: $uuid -> ${sender.path.toString()}")
      // Update peer's timestamp
      var swarm = peers(uuid)
      peers += uuid -> ((sender, System.currentTimeMillis()) ::
        swarm.filterNot(
          peer => {
            peer._1 == sender
          }
        ))
    case Prune =>
      println("Pruning..")
      // Prune inactive peers from swarms
      var prunedPeers = new HashMap[String, List[Pair[ActorRef, Long]]]()
      peers.foreach {
        case (uuid, peers) => {
          var activePeers = peers.filter(peer => peer._2 > System.currentTimeMillis()-(HeartBeat.Rate*2000))
          if (activePeers.size != 0) {
            prunedPeers += (uuid -> activePeers)
          }
        }
      }
      if (peers.size != prunedPeers.size) {
        peers = prunedPeers
      }
    case "Stop" =>
      context stop self
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
