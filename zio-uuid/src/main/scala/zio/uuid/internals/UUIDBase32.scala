package zio.uuid.internals

import java.util.UUID
import scala.annotation.switch
import scala.util.control.NonFatal

object UUIDBase32 {
  private val encodingTable: Array[Char] = "0123456789abcdefghjkmnpqrstvwxyz".toArray

  def toBase32(uuid: UUID): String = {
    @inline def enc(i: Int): Char = encodingTable(i)

    val b0 = ((uuid.getMostSignificantBits >>> 56) & 0xff).toInt
    val b1 = ((uuid.getMostSignificantBits >>> 48) & 0xff).toInt
    val b2 = ((uuid.getMostSignificantBits >>> 40) & 0xff).toInt
    val b3 = ((uuid.getMostSignificantBits >>> 32) & 0xff).toInt
    val b4 = ((uuid.getMostSignificantBits >>> 24) & 0xff).toInt
    val b5 = ((uuid.getMostSignificantBits >>> 16) & 0xff).toInt
    val b6 = ((uuid.getMostSignificantBits >>> 8) & 0xff).toInt
    val b7 = (uuid.getMostSignificantBits & 0xff).toInt

    val b8  = (uuid.getLeastSignificantBits >>> 56 & 0xff).toInt
    val b9  = (uuid.getLeastSignificantBits >>> 48 & 0xff).toInt
    val b10 = (uuid.getLeastSignificantBits >>> 40 & 0xff).toInt
    val b11 = (uuid.getLeastSignificantBits >>> 32 & 0xff).toInt
    val b12 = (uuid.getLeastSignificantBits >>> 24 & 0xff).toInt
    val b13 = (uuid.getLeastSignificantBits >>> 16 & 0xff).toInt
    val b14 = (uuid.getLeastSignificantBits >>> 8 & 0xff).toInt
    val b15 = (uuid.getLeastSignificantBits & 0xff).toInt

    val out = new Array[Char](26)
    out(0) = enc(ms3b(b0))
    out(1) = enc(ls5b(b0))
    out(2) = enc(ms5b(b1))
    out(3) = enc(ls3b(b1) << 2 | ms2b(b2))
    out(4) = enc(ls5b(b2 >> 1))
    out(5) = enc(ls1b(b2) << 4 | ms4b(b3))
    out(6) = enc(ls4b(b3) << 1 | ms1b(b4))
    out(7) = enc(ls5b(b4 >> 2))
    out(8) = enc(ls2b(b4) << 3 | ms3b(b5))
    out(9) = enc(ls5b(b5))
    out(10) = enc(ms5b(b6))
    out(11) = enc(ls3b(b6) << 2 | ms2b(b7))
    out(12) = enc(ls5b(b7 >> 1))
    out(13) = enc(ls1b(b7) << 4 | ms4b(b8))
    out(14) = enc(ls4b(b8) << 1 | ms1b(b9))
    out(15) = enc(ls5b(b9 >> 2))
    out(16) = enc(ls2b(b9) << 3 | ms3b(b10))
    out(17) = enc(ls5b(b10))
    out(18) = enc(ms5b(b11))
    out(19) = enc(ls3b(b11) << 2 | ms2b(b12))
    out(20) = enc(ls5b(b12 >> 1))
    out(21) = enc(ls1b(b12) << 4 | ms4b(b13))
    out(22) = enc(ls4b(b13) << 1 | ms1b(b14))
    out(23) = enc(ls5b(b14 >> 2))
    out(24) = enc(ls2b(b14) << 3 | ms3b(b15))
    out(25) = enc(ls5b(b15))

    new String(out)
  }

  def fromBase32(s: String): Either[String, UUID] = {
    @inline def decode(c: Char): Long =
      (c: @switch) match {
        case '0' => 0L
        case '1' => 1L
        case '2' => 2L
        case '3' => 3L
        case '4' => 4L
        case '5' => 5L
        case '6' => 6L
        case '7' => 7L
        case '8' => 8L
        case '9' => 9L
        case 'a' => 10L
        case 'b' => 11L
        case 'c' => 12L
        case 'd' => 13L
        case 'e' => 14L
        case 'f' => 15L
        case 'g' => 16L
        case 'h' => 17L
        case 'j' => 18L
        case 'k' => 19L
        case 'm' => 20L
        case 'n' => 21L
        case 'p' => 22L
        case 'q' => 23L
        case 'r' => 24L
        case 's' => 25L
        case 't' => 26L
        case 'v' => 27L
        case 'w' => 28L
        case 'x' => 29L
        case 'y' => 30L
        case 'z' => 31L
      }

    if (s.length != 26) Left("String representation should be exactly 26 significant characters")
    else {
      try {
        if (decode(s.charAt(0)) > 7) Left("The String representation encodes more than 128 bits")
        else {
          val values = s.toArray.map(decode)
          // format: off
          val msb = (values(0) << 61) | // We have only 3 significant bits at pos. 0 because of the padding
            (values(1) << 56) | (values(2) << 51) | (values(3) << 46) |
            (values(4) << 41) | (values(5) << 36) | (values(6) << 31) | (values(7) << 26) |
            (values(8) << 21) | (values(9) << 16) | (values(10) << 11) | (values(11) << 6) |
            (values(12) << 1) | ls1b((values(13) >> 4).toInt)

          val lsb = (values(13) << 60) |
            (values(14) << 55) | (values(15) << 50) | (values(16) << 45) | (values(17) << 40) |
            (values(18) << 35) | (values(19) << 30) | (values(20) << 25) | (values(21) << 20) |
            (values(22) << 15) | (values(23) << 10) | (values(24) << 5) | values(25)
          // format: on

          Right(new UUID(msb, lsb))
        }
      } catch {
        case NonFatal(_) => Left("String representation contains at least an invalid characters")
      }
    }
  }

  private val Mask1Bits: Byte = 0x01
  private val Mask2Bits: Byte = 0x03
  private val Mask3Bits: Byte = 0x07
  private val Mask4Bits: Byte = 0x0f
  private val Mask5Bits: Byte = 0x1f

  @inline
  private def ls1b(b: Int): Int =
    b & Mask1Bits

  @inline
  private def ls2b(b: Int): Int =
    b & Mask2Bits

  @inline
  private def ls3b(b: Int): Int =
    b & Mask3Bits

  @inline
  private def ls4b(b: Int): Int =
    b & Mask4Bits

  @inline
  private def ls5b(b: Int): Int =
    b & Mask5Bits

  @inline
  private def ms5b(b: Int): Int =
    (b >> 3) & Mask5Bits

  @inline
  private def ms4b(b: Int): Int =
    (b >> 4) & Mask4Bits

  @inline
  private def ms3b(b: Int): Int =
    (b >> 5) & Mask3Bits

  @inline
  private def ms2b(b: Int): Int =
    (b >> 6) & Mask2Bits

  @inline
  private def ms1b(b: Int): Int =
    (b >> 7) & Mask1Bits
}
