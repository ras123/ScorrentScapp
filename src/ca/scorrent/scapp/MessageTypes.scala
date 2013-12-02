package ca.curls.test.shared

import akka.io.Tcp

/**
 * Created with IntelliJ IDEA.
 * User: CurlyBrackets
 * Date: 11/21/13
 * Time: 5:59 PM
 * To change this template use File | Settings | File Templates.
 */
trait Request extends Tcp.Command
case class Command(msg: String) extends Request

trait Reply extends Tcp.Command
case object CommandSuccess extends Reply
case class CommandFailure(msg: String) extends Reply
