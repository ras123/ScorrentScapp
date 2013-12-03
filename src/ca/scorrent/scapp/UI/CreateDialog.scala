package ca.scorrent.scapp.UI

import scala.swing._
import javax.swing.border.EmptyBorder
import scala.swing.event.ButtonClicked
import scala.swing.GridBagPanel.{Anchor, Fill}
import javax.swing.ImageIcon
import java.io._
import scalaswingcontrib.tree.{TreeModel, Tree}
import java.security.MessageDigest
import java.lang.System
import javax.swing.filechooser.FileFilter
import java.awt.Cursor
import scala.swing.event.ButtonClicked
import java.net.URL
import scala.concurrent._
import ExecutionContext.Implicits.global

/**
 * Created with IntelliJ IDEA.
 * User: CurlyBrackets
 * Date: 11/26/13
 * Time: 3:38 PM
 * To change this template use File | Settings | File Templates.
 */
class CreateDialog extends Dialog{
  title = "Create a scorrent"
  val teName = new TextField()

  val display = new BoxPanel(Orientation.Vertical)

  contents = new BoxPanel(Orientation.Vertical){
    border = new EmptyBorder(10,10,10,10)
    contents += new BoxPanel(Orientation.Horizontal){
      contents += new Label("Name:")
      contents += teName
      maximumSize = new Dimension(Integer.MAX_VALUE,30)
    }
    contents += new Spacer(20, Orientation.Horizontal)
    //contents += new ScrollPane(display)
    contents += new ScrollPane(display)
    contents += new BoxPanel(Orientation.Horizontal){
      contents += new Button(){
        text = "Add files/folders"
        reactions += {
          case ButtonClicked(_) =>
            val fc = new FileChooser
            fc.multiSelectionEnabled = true
            fc.fileSelectionMode = FileChooser.SelectionMode.FilesAndDirectories
            if(fc.showOpenDialog(null) == FileChooser.Result.Approve){
              fc.selectedFiles.map((f: File) => {
                display.contents += new FileListView(f, CreateDialog.this)
              })
              display.revalidate()
              display.repaint()
            }
        }
      }
      contents += new Button(){
        text = "Clear list"
        reactions += {
          case ButtonClicked(_) =>
            display.contents.clear
            display.revalidate
            display.repaint
        }
      }
    }
    contents += new BoxPanel(Orientation.Horizontal){
      contents += new Button(){
        text = "Create"
        reactions += {
          case ButtonClicked(_) =>
            val fc = new FileChooser
            //TODO force extension to be .scor
            fc.fileFilter = new FileFilter {
              def getDescription: String = "Scorrent file"

              def accept(f: File): Boolean = {
                return f.isDirectory || f.getName.matches("(\\.scor)$")
              }
            }
            if(fc.showSaveDialog(null) == FileChooser.Result.Approve){
              if(Dialog.showConfirmation(message = "Files need to be moved into your download directory in order to seed\n because we're awful people who want to eat your hard drive space.\n Are you sure you wish to coninue?", title="One last thing...") == Dialog.Result.Yes){
                val files = display.contents.toList.map {
                  flv =>
                    if(flv.isInstanceOf[FileListView])
                      flv.asInstanceOf[FileListView].file
                    else
                      null
                } filter {
                  f => f != null
                }

                val progressDialog = new ProgressDialog()
                progressDialog.open()
                progressDialog.progressBar.indeterminate = true

                val total = files.foldLeft(0)((current, nFile) => {
                  current + countBytes(nFile)
                })
                progressDialog.progressBar.indeterminate = false
                progressDialog.progressBar.max = total
                progressDialog.lblStatus.text = "Copying"

                future {
                  files map {
                    f =>
                      copy(progressDialog, f,UserPrefs.get[File](DownloadDirectory).getAbsolutePath+File.separator)
                  }
                } onSuccess {
                  case _ =>
                    progressDialog.close()
                    future {
                      val xml = ScorrentParser.Build(teName.text, files)

                      val writer = new PrintWriter(fc.selectedFile)
                      writer.write(xml.mkString)
                      writer.close
                    } onSuccess {
                      case _ =>
                        Main.loadFile(fc.selectedFile)
                        Main.updateList()
                    }
                }
              }
            }

            CreateDialog.this.close()
        }
      }
      contents += new Button(){
        text = "Cancel"
        reactions += {
          case ButtonClicked(_) =>
            CreateDialog.this.close()
        }
      }
    }
  }

  size = new Dimension(300,300)

  def remove(file: File) = {
    var index:Int = 0
    var found = false
    for(c <- display.contents){
      if(!found && c.isInstanceOf[FileListView] && c.asInstanceOf[FileListView].file == file){
        found = true
      }
      else if(!found)
        index = index + 1
    }
    display.contents.remove(index)

    display.revalidate()
    display.repaint()
  }

  def countBytes(file: File): Int = {
    if(file.isDirectory){
      file.listFiles.foldLeft(0)((current, nFile) => {
        current + countBytes(nFile)
      })
    }
    else{
      val separator = File.separator
      val stream = new URL("file:"+separator+separator+file.getAbsolutePath).openStream()
      val size = stream.available()
      stream.close()
      size
    }
  }

  def copy(pd: ProgressDialog, file: File, relDir: String = ""):Unit = {
    if(file.isDirectory){
      val dir = new File(relDir + file.getName)
      dir.mkdirs()
      for(f <- file.listFiles())
        copy(pd, f, relDir + file.getName + File.separator)
    }
    else{
      val newFile = new File(relDir + file.getName)

      if(!newFile.exists()){
        pd.lblSrc.text = file.getAbsolutePath
        pd.lblDest.text = newFile.getAbsolutePath

        val is = new FileInputStream(file)
        val os = new FileOutputStream(newFile)

        var buffer: Array[Byte] = new Array[Byte](4096)
        var len = 0
        do{
          len = is.read(buffer)
          if(len != -1){
            os.write(buffer, 0, len)
            pd.progressBar.value = pd.progressBar.value + len
          }
        }while(len != -1)

        is.close()
        os.close()
      }
    }
  }
}

class FileListView(val file: File, parent: CreateDialog) extends GridBagPanel{
  border = new EmptyBorder(10,5,10,5)

  val c = new Constraints

  val icon = new Label("", new ImageIcon(if(file.isDirectory) "res/folder.png" else "res/file.png"), Alignment.Center)

  c.fill = Fill.None
  c.gridy = 0
  c.gridx = 0
  layout(icon) = c

  val lbl = new Label(file.getName)

  c.weightx = 1
  c.ipadx = 5
  c.gridy = 0
  c.gridx = 1
  c.anchor = Anchor.West
  layout(lbl) = c

  val remove = new Button(){
    icon = new ImageIcon("res/small-remove.png")
    minimumSize = new Dimension(20,20)
    preferredSize = minimumSize
    maximumSize = preferredSize
    borderPainted = false
    contentAreaFilled = false
    cursor = new Cursor(Cursor.HAND_CURSOR)
    reactions += {
      case ButtonClicked(_) =>
        parent remove file
    }
  }
  c.fill = Fill.None
  c.weightx = 0
  c.gridx = 2
  layout(remove) = c

  maximumSize = new Dimension(Int.MaxValue, 30)
}