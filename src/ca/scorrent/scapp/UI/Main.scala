package ca.scorrent.scapp.UI

import scala.swing._
import scala.swing.event._
import scala.swing.event.Key.Modifier
import javax.swing.{UIManager, JFileChooser, ImageIcon}
import scala.swing.GridBagPanel.Fill
import javax.swing.border.{EmptyBorder, LineBorder}
import java.io._
import java.security.MessageDigest
import scala.io.Source
import scala.xml.{Node, XML}
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.Some
import ca.scorrent.scapp.Model._
import scala.swing.event.MouseClicked
import scala.swing.event.ButtonClicked

/**
 * Created with IntelliJ IDEA.
 * User: CurlyBrackets
 * Date: 11/13/13
 * Time: 8:47 PM
 * To change this template use File | Settings | File Templates.
 */
object Main extends SimpleSwingApplication{
  override def main(args: Array[String]) = super.main(args)

  UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

  val lblRatio = new Label("0.24")
  val lblDownSpeed = new Label("250KB/s")
  val lblUpSpeed = new Label("10KB/s")
  var current = FilterMask.All

  lazy val allButton = new FilterButton("All"){
    reactions += {
      case ButtonClicked(_) =>
        current = FilterMask.All
        updateState()
    }
  }
  lazy val activeButton = new FilterButton("Active"){
    reactions += {
      case ButtonClicked(_) =>
        if(current == FilterMask.All)
          current = FilterMask.Active
        else
          current ^= FilterMask.Active
        updateState()
    }
  }
  lazy val dlButton = new FilterButton("Downloading"){
    reactions += {
      case ButtonClicked(_) =>
        if(current == FilterMask.All)
          current = FilterMask.Downloading
        else
          current ^= FilterMask.Downloading
        updateState()
    }
  }
  lazy val seedButton = new FilterButton("Seeding"){
    reactions += {
      case ButtonClicked(_) =>
        if(current == FilterMask.All)
          current = FilterMask.Seeding
        else
          current ^= FilterMask.Seeding
        updateState()
    }
  }
  lazy val pauseButton = new FilterButton("Paused"){
    reactions += {
      case ButtonClicked(_) =>
        if(current == FilterMask.All)
          current = FilterMask.Paused
        else
          current ^= FilterMask.Paused
        updateState()
    }
  }

  var selectedViews:List[ScorrentView] = List()
  lazy val stList = new BoxPanel(Orientation.Vertical){
    contents += new Label("Not initialized")
    listenTo(this.mouse.clicks)
    reactions += {
      case MouseClicked(_,pt,mods,_,_) =>
        val index =  pt.getY().toInt/(if(UserPrefs.get[Boolean](InfoShown)) 90 else 70)
        if(listStView.length > index){
          if(selectedViews.length != 0){
            if(Modifier.Control != mods){
              for(scv <- selectedViews)
                scv.activate()
              selectedViews = List(listStView(index))
            }
            else
              selectedViews = selectedViews :+ listStView(index)
          }
          else
            selectedViews = List(listStView(index))
          listStView(index).activate()
        }
        else if(selectedViews.length != 0){
          for(scv <- selectedViews)
            scv.activate()
          selectedViews = List[ScorrentView]()
        }
    }
  }

  var listStView = List[ScorrentView]()
  val currentlyOpen = new File("current")
  val f = future{
    if(currentlyOpen.exists){
      val root = XML.loadFile(currentlyOpen)
      for(node <- (root \ "Scorrent")){
        val file = new File(getAttribute(node, "file"))
        val fileUuid = (XML.loadFile(file) \ "UUID" head) text;
        if(fileUuid == getAttribute(node, "uuid")){
          loadFile(file)
          val scv = (listStView last)
          scv.setMode(if(getAttribute(node, "mode") == "Waiting") Waiting else Working)
        }
        else{
          Dialog.showMessage(message = "UUID of "+file.getAbsolutePath+" is altered.", title = "Error opening", icon = UIManager.getIcon("OptionPane.errorIcon"))
        }
      }
    }

    updateList()
  }

  def top = new MainFrame{
    title = "Scorrent Scapp"

    val headerButtonDimension = new Dimension(30,30)

    contents = new BoxPanel(Orientation.Vertical) {
      //header and control
      contents += new GridBagPanel(){
        val c = new Constraints
        yLayoutAlignment = 1
        maximumSize = new Dimension(Integer.MAX_VALUE, preferredSize getHeight() toInt)
        border = new EmptyBorder(20,10,5,10)
        val lButtons =  new BoxPanel(Orientation.Horizontal){
          this.xLayoutAlignment = 0
          yLayoutAlignment = 1

          contents += new HeaderButton("res/new_file.png"){
            reactions += {
              case ButtonClicked(_) =>
                val item = new CreateDialog
                item.open()
            }
          }
          contents += new HeaderButton("res/open_file.png"){
            reactions += {
              case ButtonClicked(_) =>
                val fc = new FileChooser
                fc.fileSelectionMode = FileChooser.SelectionMode.FilesOnly
                fc.multiSelectionEnabled = true
                if(fc.showOpenDialog(null) == FileChooser.Result.Approve){
                  for(f <- fc.selectedFiles)
                    loadFile(f)
                  updateList()
                }
            }
          }
          contents += new HeaderButton("res/remove.png"){
            reactions += {
              case ButtonClicked(_) =>
                Dialog.showInput(message = "Remove contents from disk as well?", title = "Remove Scorrents", entries = Seq("Yes", "No"), initial = "No") match {
                  case Some(s) =>
                    if(s == "Yes"){
                      for(scv <- selectedViews){
                          //scv.scorrent.files
                      }
                    }
                    listStView = listStView.filterNot {
                      scv =>
                        selectedViews.contains(scv)
                    }
                    updateList()
                  case None    =>
                }
            }
          }
        }
        c.weightx = 1
        c.fill = Fill.Horizontal
        c.gridx = 0
        c.gridy = 0
        layout(lButtons) = c

        val mButtons = new BoxPanel(Orientation.Horizontal){
          contents += new HeaderButton("res/pause.png"){
            reactions += {
              case ButtonClicked(_) =>
                future {
                  for(scv <- listStView){
                    scv.setMode(Waiting)
                  }
                }
            }
          }
          contents += new HeaderButton("res/restart.png"){
            reactions += {
              case ButtonClicked(_) =>
                future {
                  for(scv <- listStView){
                    scv.setMode(Waiting)
                  }
                } onSuccess {
                  case _ =>
                      for(scv <- listStView){
                        scv.setMode(Working)
                      }
                }
            }
          }
        }
        c.weightx = 1
        c.fill = Fill.Horizontal
        c.gridx = 1
        c.gridy = 0
        layout(mButtons) = c

        val rButtons = new BoxPanel(Orientation.Horizontal){
          val inactive = new ImageIcon("res/info.png")
          val active = new ImageIcon("res/info_active.png")

          contents += new HeaderButton(){
            if(UserPrefs.get[Boolean](InfoShown))
              icon = active
            else
              icon = inactive

            reactions += {
              case ButtonClicked(_) =>
                if(UserPrefs.get[Boolean](InfoShown))
                  icon = inactive
                else
                  icon = active
                UserPrefs.set(InfoShown, !UserPrefs.get[Boolean](InfoShown))
                for(scv <- listStView){
                  scv.asInstanceOf[ScorrentView].updateLayout()
                }
                stList.revalidate()
                stList.repaint()
            }
          }
          contents += new HeaderButton("res/options.png"){
            reactions += {
              case ButtonClicked(_) =>
                new PreferencesDialog().open()
            }
          }
        }
        c.weightx = 0
        c.fill = Fill.Horizontal
        c.gridx = 2
        c.gridy = 0
        layout(rButtons) = c
      }
      //speed/ratio feedback
      contents += new GridBagPanel(){
        border = new EmptyBorder(3,10,3,10)
        background = new Color(0xAEAEAE)
        yLayoutAlignment = 1
        maximumSize = new Dimension(Integer.MAX_VALUE, preferredSize getHeight() toInt)

        val c = new Constraints
        val ratPanel = new BoxPanel(Orientation.Horizontal){
          background = new Color(0xAEAEAE)
          contents += new Label("Ratio: ")
          contents += lblRatio
        }
        c.weightx = 1
        c.fill = Fill.Horizontal
        c.gridx = 0
        c.gridy = 0
        layout(ratPanel) = c

        val speedPanel = new BoxPanel(Orientation.Horizontal){
          background = new Color(0xAEAEAE)
          contents += new Label("Download: ")
          contents += lblDownSpeed
          contents += new Spacer(10, Orientation.Vertical)
          contents += new Label("Upload: ")
          contents += lblUpSpeed
        }
        c.weightx = 0
        c.fill = Fill.Horizontal
        c.gridx = 1
        c.gridy = 0
        layout(speedPanel) = c
      }
      //State panel
      contents += new GridBagPanel(){
        border = new EmptyBorder(5,10,5,10)

        yLayoutAlignment = 1
        maximumSize = new Dimension(Integer.MAX_VALUE, preferredSize getHeight() toInt)

        val c = new Constraints
        val StateButtons = new BoxPanel(Orientation.Horizontal){
          contents += allButton
          contents += activeButton
          contents += dlButton
          contents += seedButton
          contents += pauseButton

          updateState()
        }
        c.fill = Fill.Horizontal
        c.weightx = 1
        c.gridx = 0
        c.gridy = 0
        layout(StateButtons) = c
      }
      //main panel
      contents += new ScrollPane (stList)
    }
    size = new Dimension(400,300)

    reactions += {
      case WindowClosing(_) =>
        val xml =
          <OpenScorrents>
            {for (scv <- listStView) yield <Scorrent file={scv.file.getAbsolutePath} uuid={scv.scorrent.uuid} mode={if (scv.state) "Working" else "Waiting"}/>}
          </OpenScorrents>

        val writer = new PrintWriter(currentlyOpen)
        writer.write(xml.mkString)
        writer.close
    }
  }

  def updateState(): Unit = {
    if(current == FilterMask.All)
      allButton.selected = true
    else
      allButton.selected = false

    updateStateSub(FilterMask.Active, activeButton)
    updateStateSub(FilterMask.Downloading, dlButton)
    updateStateSub(FilterMask.Seeding, seedButton)
    updateStateSub(FilterMask.Paused, pauseButton)
  }
  def updateStateSub(state: Int, button: FilterButton) = {
    if((current & state) != 0){
        button.selected = current != FilterMask.All
        //do stuff with things
    }
  }

  def loadFile(ofile: File) = {
    var file = ofile
    if(UserPrefs.get[Boolean](Backup) && !file.getAbsolutePath.contains(UserPrefs.get[File](ScorDirectory).getAbsolutePath)){
      val newFile = new File(UserPrefs.get[File](ScorDirectory).getAbsolutePath + File.separator + file.getName)

      var buffer: Array[Byte] = new Array[Byte](4096) // fuck you arbitrary values are great
      val is = new FileInputStream(file)
      val os = new FileOutputStream(newFile)
      var length:Int = -1

      do{
        length = is.read(buffer)
        if(length != -1)
          os.write(buffer, 0, length)
      }while(length != -1)

      is.close()
      os.close()
      file = newFile
    }
    val scor = ScorrentParser.Load(file)
    if(scor != null){
      if(listStView.foldLeft(false)(
          (current, scv) =>
            current || scv.scorrent.uuid == scor.uuid)){
        Dialog.showMessage(message = "Scorrent "+scor.name+" is already added to the list", title = "Error opening", icon = UIManager.getIcon("OptionPane.errorIcon"))
      }
      else{
        listStView = listStView :+ new ScorrentView(scor, file)
      }
    }
  }

  def getAttribute(node: Node, key: String) = {
    node.attribute(key) match {
      case Some(s) =>
        s.head.text
      case None =>
        throw new Exception("Error parsing. Key not found: "+key)
    }
  }

  def updateList() = {
    if(listStView.isEmpty){
      stList.contents.clear()
      stList.contents += new Label("No scorrents added")
    }
    else{
      stList.contents.clear()
      listStView map {
        scv => stList.contents += scv
      }
    }
    stList.revalidate()
    stList.repaint()

    future {
      val xml =
        <OpenScorrents>
          {for(scv <- listStView) yield <Scorrent file={scv.file.getAbsolutePath} uuid={scv.scorrent.uuid} mode={if(scv.state) "Working" else "Waiting"}/>}
        </OpenScorrents>

      val writer = new PrintWriter(currentlyOpen)
      writer.write(xml.mkString)
      writer.close
    }
  }
}
