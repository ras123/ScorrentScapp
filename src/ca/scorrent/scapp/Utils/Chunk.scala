package ca.scorrent.scapp.Utils

import java.io.{RandomAccessFile, File}

/**
 * Created with IntelliJ IDEA.
 * User: Kyle
 * Date: 12/2/2013
 * Time: 7:36 PM
 * To change this template use File | Settings | File Templates.
 */
class Chunk(var byteArray : Array[Byte], val offset : Int, val hash : String, var exists : Boolean) {
  def verifyChunk() {
    hash.equals(FileHasher.getDatDankHash(byteArray))
  }

  /**
   * Writes the chunk's byte array to the file given its offset. Haven't tested ohohoho
   * @param destFile
   */
  def writeChunk(destFile : File) {
    val file = new RandomAccessFile(destFile, "rw")
    file.seek(offset * Constants.CHUNKSIZE)
    file.write(byteArray);
    file.close
  }
}
