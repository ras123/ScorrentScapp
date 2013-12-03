package ca.scorrent.scapp.UI

import scala.swing._
import javax.swing.{Icon, ImageIcon}
import java.awt.{BasicStroke, Cursor, Font}
import scala.swing.event.{MouseReleased, MousePressed, MouseExited, MouseEntered}
import javax.swing.border.EmptyBorder

/**
 * Created with IntelliJ IDEA.
 * User: CurlyBrackets
 * Date: 11/14/13
 * Time: 4:08 PM
 * To change this template use File | Settings | File Templates.
 */
class HeaderButton(iconPath: String) extends Button{
  def this() = this("")

  cursor = new Cursor(Cursor.HAND_CURSOR)
  if(!iconPath.isEmpty)
    icon = new ImageIcon(iconPath)
  minimumSize = new Dimension(35,35)
  preferredSize = minimumSize
  maximumSize = preferredSize
  border = new EmptyBorder(5,5,5,5)

  var drawMode: DrawMode = Normal
  var nextState: DrawMode = Normal

  override def paint(g: Graphics2D) = {
    val width = size.getWidth().toInt
    val height = size.getHeight().toInt
    drawMode match {
      case Normal =>
        g.setColor(new Color(0xE2E2E2))
      case Hover =>
        g.setColor(new Color(0xF5F5F5))
      case Press =>
        g.setColor(new Color(0xC0C0C0))
    }
    g.fillRect(0, 0, width, height);
    // selected foreground color
    icon.paintIcon(null,g,
      width/2 - icon.getIconWidth()/2,
      height/2- icon.getIconHeight()/2);
    drawMode match {
      case Normal =>
        g.setColor(new Color(0xC2C2C2))
      case Hover =>
        g.setColor(new Color(0xE2E2E2))
      case Press =>
        g.setColor(new Color(0xFFFFFF))
    }
    g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND))
    g.drawRect(1,1,width-2,height-2)
  }
  listenTo(this.mouse.moves)
  listenTo(this.mouse.clicks)
  reactions += {
    case MouseEntered(_, _, _) =>
      drawMode = Hover
    case MouseExited(_, _, _) =>
      drawMode match {
        case Press =>
          nextState = Normal
        case _ =>
          drawMode = Normal
      }
    case MousePressed(_,_,_,_,_) =>
      nextState = drawMode
      drawMode = Press
    case MouseReleased(_,_,_,_,_) =>
      drawMode = nextState
  }
}

sealed trait DrawMode
case object Normal extends DrawMode
case object Hover  extends DrawMode
case object Press  extends DrawMode