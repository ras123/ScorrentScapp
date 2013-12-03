package ca.scorrent.scapp.UI
import java.io.File
import java.security.MessageDigest
import sun.misc.BASE64Encoder
import scala.xml._
import ca.scorrent.scapp.Model.Scorrent
import scala.swing.Dialog
import javax.swing.UIManager

/**
 * Created with IntelliJ IDEA.
 * User: CurlyBrackets
 * Date: 11/29/13
 * Time: 10:10 PM
 * To change this template use File | Settings | File Templates.
 */
object ScorrentParser {
  def Build(name: String, files: List[File]) = {
    <Scorrent>
      <Name>{name}</Name>
      <UUID>{new BASE64Encoder().encode(MessageDigest.getInstance("SHA-256").digest(name.getBytes()) ++ LongToBytes(System.currentTimeMillis()))}</UUID>
      <Tracker>localhost:1337</Tracker>{/*TODO get the bound port and address from akka or some shit */ }
      <ChunkSize>4096</ChunkSize>{/*TODO Get some Kit Kat up in here */ }
      <Files>
        {for(f <- files) yield createXMLFile(f)}
      </Files>
    </Scorrent>
  }

  def Load(file: File): Scorrent = {
    try{
      val root = XML.loadFile(file)

      val name = (root \\ "Name" head) text
      val tracker = (root \\ "Tracker" head) text
      val uuid = (root \\ "UUID" head) text

      //TODO Actually load the scorrent in, just not a fraction of thigns

      new Scorrent(name, tracker, uuid)
    }
    catch{
      case ex: Throwable => //TODO Should probably change this up, was a just a lazy thing
        Dialog.showMessage(message = ex.getMessage, title = "Error opening", icon = UIManager.getIcon("OptionPane.errorIcon"))
        null
    }
  }

  //for the uuid
  private def LongToBytes(l: Long) = {
    val ret: Array[Byte] = new Array[Byte](8)
    for(i <- 0 to 7)
      ret(i) = ((l >> i*8) & 0xFF).asInstanceOf[Byte]
    ret
  }

  //recursively build file
  private def createXMLFile(file: File, relDir: String = ""): Elem = {
    if(file.isDirectory){
      <Folder>
        {
          for(f <- file.listFiles)
            yield createXMLFile(f, relDir+file.getName+"/")

        }
      </Folder>
    }
    else{
      <File name={relDir+file.getName} hash="baadf00d">{/*TODO: get datdankhash for ehre and get some chunky fun times below*/}
        <ChunkyPlacehodler/>
      </File>
    }
  }
}
