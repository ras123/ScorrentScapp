package ca.scorrent.scapp.Model

/**
 * Created with IntelliJ IDEA.
 * User: CurlyBrackets
 * Date: 11/14/13
 * Time: 2:24 PM
 * To change this template use File | Settings | File Templates.
 */

object FilterMask{
  val Active = 1
  val Downloading = 2
  val Seeding = 4
  val Paused = 8
  val All = 15
}

sealed trait ScorrentState
case object Working extends ScorrentState
case object Waiting extends ScorrentState
case object Error extends ScorrentState

sealed trait SizeUnit
case object MB extends SizeUnit
case object KB extends SizeUnit
case object B extends SizeUnit