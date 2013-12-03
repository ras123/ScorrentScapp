package ca.scorrent.scapp.Services

import akka.io.Tcp
import akka.actor.ActorPath

/**
 * Created with IntelliJ IDEA.
 * User: CurlyBrackets
 * Date: 12/2/13
 * Time: 4:09 PM
 * To change this template use File | Settings | File Templates.
 */
sealed trait TrackerMessage extends Tcp.Command
case object CheckIn extends TrackerMessage
case object Success extends TrackerMessage
case object PeerUpdate extends TrackerMessage
case class Peers(peers: List[ActorPath]) extends TrackerMessage
case object PeerRequest extends TrackerMessage
case class NewPeers(np: Boolean) extends TrackerMessage
case object Prune extends TrackerMessage
case class Register(uuid: String) extends TrackerMessage