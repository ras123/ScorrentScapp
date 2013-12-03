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
    var chunks = Vector[Array[Byte]]()

    val plzChunkMe = com.google.common.io.Files toByteArray file

    val numChunks =
      if (plzChunkMe.length % Constants.CHUNKSIZE == 0)
        plzChunkMe.length / Constants.CHUNKSIZE
      else
        plzChunkMe.length / Constants.CHUNKSIZE + 1

    for (i <- 0 to numChunks - 1) {
      val baseIndex = i * Constants.CHUNKSIZE
      val chunk = plzChunkMe slice(baseIndex, baseIndex + Constants.CHUNKSIZE)
      chunks = chunks :+ chunk
    }
    chunks
  }
}
