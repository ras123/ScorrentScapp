package ca.scorrent.scapp.Utils

import java.io.{RandomAccessFile, File}

/**
 * Created with IntelliJ IDEA.
 * User: Kyle
 * Date: 12/2/2013
 * Time: 7:36 PM
 * To change this template use File | Settings | File Templates.
 */
class ChunkWriter(val destFile: File) {
  private var chunksWritten = 0

  def verifyChunk(byteArray: Array[Byte], hash: String) = {
    hash.equals(FileHasher.getDatDankHash(byteArray))
  }

  /**
   * Writes the chunk's byte array to the file given its offset. Haven't tested ohohoho
   */
  def writeChunk(byteArray: Array[Byte], offset: Int) {
    val file = new RandomAccessFile(destFile, "rw")
    file.seek(offset * Constants.CHUNKSIZE)
    file.write(byteArray);
    file.close

    chunksWritten += 1
  }

  def chunksWrittenCount = chunksWritten
}
