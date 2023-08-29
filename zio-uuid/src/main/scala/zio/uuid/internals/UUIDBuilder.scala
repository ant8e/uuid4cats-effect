package zio.uuid.internals

import zio.uuid.types.{UUIDv1, UUIDv6, UUIDv7}

import java.util.UUID

private[zio] object UUIDBuilder {
  val Variant = 0x2L

  def buildUUIDv1(epochMillis: Long, sequence: Long, random: Long): UUIDv1 = {
    val Version            = 0x1L
    val gregorianTimestamp = toUUIDTimestamp(epochMillis)
    val time_high          =
      gregorianTimestamp >>> 48 // 12 most significant bits of the timestamp
    val time__mid =
      (gregorianTimestamp >>> 32) & 0xffff // 16 middle bits of the timestamp
    val time_low =
      gregorianTimestamp & 0xffff_ffff // 32 least significant bits of the timestamp
    val node =
      ((random << 16) >>> 16) | (0x1L << 40) // 48 bits (MAC address with the unicast bit set to 1)
    val clock_seq = sequence & 0x3fff // 14 bits
    val msb       = (time_low << 32) | time__mid << 16 | (Version << 12) | time_high
    val lsb       = (Variant << 62) | clock_seq << 48 | node
    UUIDv1.unsafe(new UUID(msb, lsb))
  }

  def buildUUIDv6(epochMillis: Long, sequence: Long, random: Long): UUIDv6 = {
    val Version            = 0x6L
    val gregorianTimestamp = toUUIDTimestamp(epochMillis)
    val time_high_and_mid  =
      gregorianTimestamp >>> 12 // 48 most significant bits of the timestamp
    val time_low =
      gregorianTimestamp & 0xfff // 12 least significant bits of the timestamp
    val node      = (random << 16) >>> 16 // 48 bits
    val clock_seq = sequence & 0x3fff     // 14 bits
    val msb       = (time_high_and_mid << 16) | (Version << 12) | time_low
    val lsb       = (Variant << 62) | clock_seq << 48 | node
    UUIDv6.unsafe(new UUID(msb, lsb))
  }

  def buildUUIDv7(epochMillis: Long, sequence: Long, random: Long): UUIDv7 = {
    val Version = 0x7L
    val rand_a  = sequence & 0xfffL   // 12 bits
    val rand_b  = (random << 2) >>> 2 // we need only 62 bits of randomness
    val msb     = (epochMillis << 16) | (Version << 12) | rand_a
    val lsb     = (Variant << 62) | rand_b
    UUIDv7.unsafe(new UUID(msb, lsb))
  }

  /**
   * number of 100 nanosecond intervals since the beginning of the gregorian
   * calendar (15-oct-1582) to Unix Epoch
   */
  private val UnixEpochClockOffset = 0x01b21dd213814000L

  @inline
  final def toUUIDTimestamp(epochMillis: Long): Long = {
    val ClockMultiplier = 10000L //  count of 100 nanosecond intervals in a milli
    val ts              = epochMillis * ClockMultiplier + UnixEpochClockOffset
    (ts << 4) >>> 4 // Keeping only the 60 least significant bits
  }
}
