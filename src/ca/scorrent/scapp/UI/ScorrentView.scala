package ca.scorrent.scapp.UI
import scala.swing._
import java.awt.{Cursor, Dimension}
import javax.swing.border.EmptyBorder
import javax.swing.ImageIcon
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.swing.GridBagPanel.{Anchor, Fill}
import ca.scorrent.scapp.Model._
import scala.swing.event.ButtonClicked
import scala.swing.Color
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: CurlyBrackets
 * Date: 11/14/13
 * Time: 4:39 PM
 * To change this template use File | Settings | File Templates.
 */
class ScorrentView(val scorrent: Scorrent, val file: File) extends GridBagPanel{
  val c = new Constraints
  border = new EmptyBorder(5,10,5,10)

  var state = false
  var active = false

  val ppButton = new Button(){
    cursor = new Cursor(Cursor.HAND_CURSOR)
    minimumSize = new Dimension(20,20)
    preferredSize = minimumSize
    maximumSize = preferredSize
    borderPainted = false
    contentAreaFilled = false

    icon = ScorrentView.pauseIcon
    reactions += {
      case ButtonClicked(_) =>
        icon = ScorrentView.workIcon
        var targetState: ScorrentState = null
        if(state)
          targetState = Waiting
        else
          targetState = Working
        setMode(targetState)
    }
  }
  val progressBar = new ProgressBar(){
    max = 1000
    value = 450
  }
  val lblName = new Label(scorrent.name)

  updateLayout()

  def updateLayout() = {
    layout.clear()

    var targetSize = 90
    var rowCount:Int = 4
    if(!UserPrefs.get[Boolean](InfoShown)){
      targetSize = 50
      rowCount = 3
    }

    minimumSize = new Dimension(0,targetSize)
    maximumSize = new Dimension(Int.MaxValue, targetSize)

    c.fill = Fill.None
    c.weightx = 0
    c.ipadx = 5
    c.ipady = 5
    c.gridx = 0
    c.gridy = rowCount/2
    layout(ppButton) = c

    c.fill = Fill.Horizontal
    c.weightx = 1
    c.gridx = 1
    c.gridy = rowCount/2
    layout(progressBar) = c

    c.fill = Fill.None
    c.gridy = 0
    c.anchor = Anchor.West
    layout(lblName) = c

    if(UserPrefs.get[Boolean](InfoShown)){
      //TODO also attach rate update listener things
      val sizeLabel = new Label("0")
      val sizeBox = new BoxPanel(Orientation.Horizontal){
        background = new Color(0,0,0,0)
        //TODO: Get from file sizes
        val sizeUnit = new Label("MB(s)")
        contents += sizeLabel
        contents += new Label(" of ")
        contents += new Label(scorrent.size(MB).toString()+" ")
        contents += sizeUnit
      }
      c.gridy = 1
      c.anchor = Anchor.West
      layout(sizeBox) = c

      val peers = new Label("0")
      val seeds = new Label("0")
      val dl = new Label("544")
      val ul = new Label("9.6")

      val statsBox = new BoxPanel(Orientation.Horizontal){
        background = new Color(0,0,0,0)
        contents += new Label("Peers: ")
        contents += peers
        contents += new Label("Seeds: ")
        contents += seeds
        contents += new Spacer(30, Orientation.Vertical)
        contents += new Label("Download: ")
        contents += dl
        contents += new Label("KB/s  ")
        contents += ul
        contents += new Label("KB/s")
      }

      c.gridy = 3
      c.anchor = Anchor.West
      layout(statsBox) = c
    }
  }
  def activate() = {
    active = !active
    if(active)
      background = new Color(0xD0D0D0)
    else
      background = new Color(0xF0F0F0)
  }

  def setMode(newState: ScorrentState): Unit = {
    scorrent.changeState(newState) onSuccess {
      case Working =>
        ppButton.icon = ScorrentView.playIcon
        state = true
      case Waiting =>
        ppButton.icon = ScorrentView.pauseIcon
        state = false
      case Error => ppButton.icon = ScorrentView.errorIcon
    }
  }
}

object ScorrentView{
  val playIcon = new ImageIcon("res/play.png")
  val pauseIcon = new ImageIcon("res/small-pause.png")
  val workIcon = new ImageIcon("res/work.png")
  val errorIcon = new ImageIcon("res/error.png")
}
