package ca.scorrent.scapp.UI
import scala.swing._

/**
 * Created with IntelliJ IDEA.
 * User: CurlyBrackets
 * Date: 12/2/13
 * Time: 12:46 PM
 * To change this template use File | Settings | File Templates.
 */
class ProgressDialog() extends Dialog{
  title = "Copying files"

  lazy val lblStatus = new Label("Initializing...")
  lazy val lblSrc = new Label("")
  lazy val lblDest = new Label("")

  lazy val progressBar = new ProgressBar(){
    max = 100
    value = 0
  }

  contents = new BoxPanel(Orientation.Vertical){
    contents += lblStatus
    contents += new BoxPanel(Orientation.Horizontal){
      contents += new Label("Source: ")
      contents += lblSrc
    }
    contents += new BoxPanel(Orientation.Horizontal){
      contents += new Label("Destination: ")
      contents += lblDest
    }
    contents += progressBar
  }

  size = new Dimension(400,100)
}
