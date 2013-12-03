package ca.scorrent.scapp.UI
import scala.swing.Separator
import scala.swing._

/**
 * Created with IntelliJ IDEA.
 * User: CurlyBrackets
 * Date: 11/14/13
 * Time: 2:01 PM
 * To change this template use File | Settings | File Templates.
 */
class Spacer(width: Int, o: Orientation.Value) extends Separator(o){
  val dim = new Dimension(width, 1)
  preferredSize = dim
  minimumSize = dim
  maximumSize = dim
  val transparent = new Color(0,0,0,0)
  background = transparent
  foreground = transparent
}
