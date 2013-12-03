package ca.scorrent.scapp.UI
import scala.swing._
import java.io.File
import javax.swing.border.EmptyBorder
import scalaswingcontrib.group.GroupPanel
import scala.swing.event.ButtonClicked

/**
 * Created with IntelliJ IDEA.
 * User: CurlyBrackets
 * Date: 11/28/13
 * Time: 2:09 PM
 * To change this template use File | Settings | File Templates.
 */
class PreferencesDialog extends Dialog{
  title = "Settings"
  contents = new BoxPanel(Orientation.Vertical){
    val lbldlDir = new Label("Download Directory: ")
    val lblscDir = new Label("Scorrent Directory: ")
    val lblPort = new Label("Port: ")
    val lblBackup = new Label("Backup: ")
    val tedlDir = new TextField(UserPrefs.get[File](DownloadDirectory).getAbsolutePath)
    val tescDir = new TextField(UserPrefs.get[File](ScorDirectory).getAbsolutePath){
      enabled = UserPrefs.get[Boolean](Backup)
    }
    val tePort = new TextField(UserPrefs.get[Int](PortNumber).toString)
    val cbBackup = new CheckBox("Backup scorrents to local directory"){
      selected = UserPrefs.get[Boolean](Backup)
      reactions += {
        case ButtonClicked(_) =>
          tescDir.enabled = selected
          UserPrefs.set(Backup, selected)
      }
    }

    border = new EmptyBorder(5,5,5,5)
    contents += new GroupPanel(){
      autoCreateGaps = true

      theHorizontalLayout is Parallel(
        Sequential(
          lbldlDir,
          PreferredGap(Related),
          tedlDir
        ),
        Sequential(
          lblBackup,
          PreferredGap(Related),
          cbBackup
        ),
        Sequential(
          lblscDir,
          PreferredGap(Related),
          tescDir
        ),
        Sequential(
          lblPort,
          PreferredGap(Related),
          tePort
        )
      )
      theVerticalLayout is Sequential(
        Parallel(
          lbldlDir,
          tedlDir
        ),
        Parallel(
          lblBackup,
          cbBackup
        ),
        Parallel(
          lblscDir,
          tescDir
        ),
        Parallel(
          lblPort,
          tePort
        )
      )
      linkHorizontalSize(lbldlDir,lblscDir,lblPort)
    }


    contents += new BoxPanel(Orientation.Horizontal){
      contents += new Button("Save"){
        xLayoutAlignment = 1
        reactions += {
          case ButtonClicked(_) =>
            UserPrefs.set(DownloadDirectory, new File(tedlDir.text))
            UserPrefs.set(ScorDirectory, new File(tescDir.text))
            UserPrefs.set(PortNumber, tePort.text.toInt)
            PreferencesDialog.this.close()
        }
      }
      contents += new Button("Close"){
        xLayoutAlignment = 1
        reactions += {
          case ButtonClicked(_) =>
            PreferencesDialog.this.close()
        }
      }
    }
  }
}
