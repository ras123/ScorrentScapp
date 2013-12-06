package ca.scorrent.scapp.Utils

import java.io.File
import scala.xml._
import ca.scorrent.scapp.Model.Scorrent
import scala.swing.Dialog
import javax.swing.UIManager
import scala.util.Random
import scala.collection.mutable.ListBuffer

/**
 * Created with IntelliJ IDEA.
 * User: CurlyBrackets
 * Date: 11/29/13
 * Time: 10:10 PM
 * To change this template use File | Settings | File Templates.
 */
object ScorrentParser {
  def Build(name: String, tracker: String, files: List[File]) = {
    <Scorrent>
      <Name>{name}</Name>
      <UUID>{FileHasher.getDatDankHashForNewFileName(name.getBytes)}</UUID>
      <Tracker>{tracker}</Tracker>
      <ChunkSize>{Constants.CHUNKSIZE}</ChunkSize>{/*TODO Get some Kit Kat up in here */ }
      <Files>
        {for(f <- files) yield createXMLFile(f)}
      </Files>
    </Scorrent>
  }

  def getAttribute(node: Node, key: String) = {
    node.attribute(key) match {
      case Some(s) =>
        s.head.text
      case None =>
        throw new Exception("Error parsing. Key not found: "+key)
    }
  }

  def Load(file: File): Scorrent = {
    try{
      val root = XML.loadFile(file)

      val name = (root \\ "Name" head) text
      val tracker = (root \\ "Tracker" head) text
      val uuid = (root \\ "UUID" head) text

      //TODO Actually load the scorrent in, just not a fraction of thigns
      val numOfChunks = (root \\ "NumOfChunks" head) text
      var files = Vector[String]()
      for(fileNode <- (root \\ "File")) {
        var fileName = getAttribute(fileNode, "name")
        files =  files :+ fileName
      }

      var chunkHashes = Vector[String]()
      for (hash <- (root \\ "Chunk")) {
        chunkHashes = chunkHashes :+ getAttribute(hash, "hash")
      }

      new Scorrent(name, tracker, uuid, numOfChunks.toInt, files, chunkHashes)
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
    val chunks = FileChunker.getChunks(file).toArray
    if(file.isDirectory){
      <Folder>
        {
          for(f <- file.listFiles)
            yield createXMLFile(f, relDir+file.getName+"/")

        }
      </Folder>
    }
    else{
      <File name={relDir+file.getName} hash={FileHasher.getDatDankHash(file)}>
      <NumOfChunks>{chunks.length}</NumOfChunks>
        {
          for(i <- 0 until chunks.length)
            yield createXMLChunk(chunks(i), i)
        }
      </File>
    }
  }

  private def createXMLChunk(chunk : Array[Byte], index : Int) : Elem = {
    val test = <Chunk index={index.toString} hash={FileHasher.getDatDankHash(chunk)}>
    </Chunk>
    test
  }
}

// This is just for local testing
object ScorrentParserDriver extends App {
  val scor = ScorrentParser.Load(new File("/home/ras/ScorrentScapp/scors/file.scor"))
  var chunks = FileChunker.getChunks(new File("file.txt"))
}