package ca.scorrent.scapp.UI

import scala.swing._
import javax.swing.border.LineBorder
import java.awt.{Cursor, Font, SystemColor, Color}
import scala.swing.event.{ValueChanged, BackgroundChanged}


/**
 * Created with IntelliJ IDEA.
 * User: CurlyBrackets
 * Date: 11/14/13
 * Time: 2:26 PM
 * To change this template use File | Settings | File Templates.
 */
class FilterButton(txt: String) extends ToggleButton(txt){
  val unselCol = new Color(0xE2E2E2)
  val selCol   = new Color(0x929292)
  val selTextCol = new Color(0xF5F5F5)
  val unselTextCol = new Color(0x101010)
  val paddingx = 10
  val paddingy = 5

  cursor = new Cursor(Cursor.HAND_CURSOR)
  border = new LineBorder(new Color(0xC0C0C0),1,false)
  minimumSize = new Dimension(preferredSize.getWidth().toInt+paddingx,
                              preferredSize.getHeight().toInt+paddingy)
  preferredSize = minimumSize
  maximumSize = preferredSize

  override def paint(g: Graphics2D) = {
    val width = size.getWidth().toInt
    val height = size.getHeight().toInt
    // selected color
    g.setColor(if(selected) selCol else unselCol)
    g.fillRect(0, 0, width, height);
    // selected foreground color
    g.setColor(if(selected) selTextCol else unselTextCol)
    g.setFont(g.getFont().deriveFont(Font.BOLD))
    g.drawString(text,
      (width - g.getFontMetrics().stringWidth(text)) / 2 + 1,
      (height + g.getFontMetrics().getAscent()) / 2 - 1);

    this.paintBorder(g)
  }
}
