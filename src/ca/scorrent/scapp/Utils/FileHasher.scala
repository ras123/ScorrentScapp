package ca.scorrent.scapp.Utils

import sun.misc.BASE64Encoder
import java.security.MessageDigest
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: Kyle
 * Date: 12/2/2013
 * Time: 6:32 PM
 * To change this template use File | Settings | File Templates.
 *
 * Can create a SHA-256 hash digest from byte arrays.
 */
object FileHasher {

  def getDatDankHashForNewFileName(byteArray : Array[Byte]): String = {
    getDatDankHash(byteArray ++ LongToBytes(System.currentTimeMillis()))
  }

  def getDatDankHash(byteArray : Array[Byte]): String = {
    new BASE64Encoder().encode(MessageDigest.getInstance("SHA-256").digest(byteArray))
  }

  def getDatDankHash(file : File): String = {
    val bytes = com.google.common.io.Files toByteArray file
    getDatDankHash (bytes)
  }

  //for the uuid
  private def LongToBytes(l: Long) = {
    val ret: Array[Byte] = new Array[Byte](8)
    for(i <- 0 to 7)
      ret(i) = ((l >> i*8) & 0xFF).asInstanceOf[Byte]
    ret
  }

}
