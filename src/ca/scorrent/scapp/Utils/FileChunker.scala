package ca.scorrent.scapp.Utils

import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: Kyle
 * Date: 12/2/2013
 * Time: 6:28 PM
 * To change this template use File | Settings | File Templates.
 *
 * Chunks a file.
 */
object FileChunker {

  def getChunks(file: File) : Vector[Array[Byte]] = {
    val CHUNK_LENGTH = 10
    var chunks = Vector[Array[Byte]]()

    val plzChunkMe = com.google.common.io.Files toByteArray file

    val numChunks =
      if (plzChunkMe.length % CHUNK_LENGTH == 0)
        plzChunkMe.length / CHUNK_LENGTH
      else
        plzChunkMe.length / CHUNK_LENGTH + 1

    for (i <- 0 to numChunks - 1) {
      val baseIndex = i * CHUNK_LENGTH
      val chunk = plzChunkMe slice(baseIndex, baseIndex + CHUNK_LENGTH)
      chunks = chunks :+ chunk
    }
    chunks
  }
}
