package ca.scorrent.scapp.Services

import akka.actor.{ActorRef, Actor}
import scala.collection.immutable.HashMap

/**
 * Created with IntelliJ IDEA.
 * User: CurlyBrackets
 * Date: 12/2/13
 * Time: 4:15 PM
 * To change this template use File | Settings | File Templates.
 */
class Tracker extends Actor{
  var peers: Map[ActorRef,Long] = new HashMap[ActorRef,Long]()
  var oldTimes: Map[ActorRef, Long] = new HashMap[ActorRef, Long]()

  def receive = {
    case CheckIn =>
      oldTimes = oldTimes + (sender -> peers(sender))
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
      sender ! Peers(peers.keys.toList.map( (ref) => {ref.path}))
    case Prune =>
      peers = peers.filter(
        (pair) => {
          pair._2 > System.currentTimeMillis()-(HeartBeat.Rate*1000)
        }
      )
    case _ =>
      println("Fuck off wrong thing sent")
  }
}
