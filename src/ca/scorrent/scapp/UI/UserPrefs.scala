package ca.scorrent.scapp.UI

import java.io.{PrintWriter, File}
import scala.collection.mutable.Map
import scala.collection.mutable.HashMap
import scala.xml.XML
import scala.concurrent._
import ExecutionContext.Implicits.global

/**
 * Created with IntelliJ IDEA.
 * User: CurlyBrackets
 * Date: 28/11/13
 * Time: 12:05 AM
 * To change this template use File | Settings | File Templates.
 */
object UserPrefs {
  val file = new File("prefs")
  val map: Map[Key, Any] = new HashMap[Key,Any]
  if(file.exists){
    val root = XML.loadFile(file)
    map put(DownloadDirectory, new File((root \ "DlDir" head) text))
    map put(ScorDirectory, new File((root \ "ScDir" head) text))
    map put(InfoShown, ((root \ "Info" head) text) == "true")
    map put(PortNumber, ((root \ "Port" head) text) toInt)
    map put(Backup, ((root \ "Backup" head) text) == "true")

  }
  else{
    val dlDir = new File("downloads")
    if(!dlDir.exists){
      if(!dlDir.mkdir())
        throw new Exception("Something fucked up")
    }

    val scDir = new File("scors")
    if(!scDir.exists){
      if(!scDir.mkdir)
        throw new Exception("fuck you")
    }

    map put(DownloadDirectory, dlDir)
    map put(Backup, true)
    map put(ScorDirectory, scDir)
    map put(InfoShown, false)
    map put(PortNumber, 0)
    save()
  }

  def save() = {
    future {
      val xml =
      <Root>
        <DlDir>{get(DownloadDirectory)}</DlDir>
        <ScDir>{get(ScorDirectory)}</ScDir>
        <Info>{get(InfoShown).toString}</Info>
        <Port>{get(PortNumber)}</Port>
        <Backup>{get(Backup)}</Backup>
      </Root>

      val writer = new PrintWriter(file)
      writer.write(xml.mkString)
      writer.close
    }
  }

  def get[T](key: Key) = {
    map.get(key) match {
      case Some(v) => v.asInstanceOf[T]
      case None => throw new Exception("Missing key: "+key)
    }
  }

  def set(key:Key, value: Any) = {
    map.put(key,value)
    save()
  }
}

sealed trait Key
case object DownloadDirectory extends Key
case object Backup extends Key
case object ScorDirectory extends Key
case object InfoShown extends Key
case object PortNumber extends Key
