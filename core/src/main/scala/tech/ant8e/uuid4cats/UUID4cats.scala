/*
 * Copyright 2023 Ant8e
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.ant8e.uuid4cats

import cats.effect.std.{Mutex, Random, SecureRandom}
import cats.effect.{Async, Clock, Ref}
import cats.implicits._

import java.util.UUID

trait UUIDGenerator[F[_]] {
  def uuid: F[UUID]
}

object UUIDv1 extends TimestampedUUIDGeneratorBuilder {

  /** return a UUIDv1 (gregorian timestamp based, non-sortable) generator with
    * guarantee about the uniqueness of the UUID, even within the same
    * millisecond timestamp.
    *
    * This function uses a randomized MAC address.
    */
  def generator[F[_]: Async]: F[UUIDGenerator[F]] =
    buildGenerator(UUIDBuilder.buildUUIDv1)
}

object UUIDv4 {

  /** return a UUIDv4 (full random) generator. */
  def generator[F[_]: Async]: F[UUIDGenerator[F]] = SecureRandom
    .javaSecuritySecureRandom[F]
    .map(random =>
      new UUIDGenerator[F] {
        override def uuid: F[UUID] = for {
          low <- random.nextLong
          high <- random.nextLong
        } yield UUIDBuilder.buildUUIDv4(high, low)
      }
    )
}

object UUIDv6 extends TimestampedUUIDGeneratorBuilder {

  /** return a UUIDv6 (gregorian timestamp based, sortable) generator with
    * guarantee about the uniqueness of the UUID, even within the same
    * millisecond timestamp.
    */
  def generator[F[_]: Async]: F[UUIDGenerator[F]] =
    buildGenerator(UUIDBuilder.buildUUIDv6)
}

object UUIDv7 extends TimestampedUUIDGeneratorBuilder {

  /** return a UUIDv7 (unix epoch timestamp based, sortable) generator with
    * guarantee about the uniqueness of the UUID, even within the same
    * millisecond timestamp.
    */
  def generator[F[_]: Async]: F[UUIDGenerator[F]] =
    buildGenerator(UUIDBuilder.buildUUIDV7)
}

sealed trait TimestampedUUIDGeneratorBuilder {
  private type UUIDBuilder = (Long, Long, Long) => UUID

  private type GeneratorState = (Long, Long) // Last used Epoch millis, sequence

  private def generate[F[_]: Async](
      state: Ref[F, GeneratorState],
      mutex: Mutex[F],
      random: Random[F],
      builder: UUIDBuilder
  ): F[UUID] = for {
    random <- random.nextLong
    uuid <- mutex.lock.surround(for {
      timestamp <- Clock[F].realTime
      sequence <- state.modify { case (previousTimestamp, previousSeq) =>
        val timestampAsMillis = timestamp.toMillis
        val seq =
          if (previousTimestamp === timestampAsMillis) previousSeq + 1 else 0L
        ((timestampAsMillis, seq), seq)
      }
    } yield builder(timestamp.toMillis, sequence, random))
  } yield uuid

  protected def buildGenerator[F[_]: Async](
      builder: UUIDBuilder
  ): F[UUIDGenerator[F]] = {
    val generatorInitialState = Ref[F].of(0L -> 0L)
    for {
      state <- generatorInitialState
      mutex <- Mutex[F]
      random <- SecureRandom.javaSecuritySecureRandom[F]
    } yield new UUIDGenerator[F] {
      override def uuid: F[UUID] = generate(state, mutex, random, builder)
    }
  }
}

object UUIDBuilder {
  val Variant = 0x2L

  def buildUUIDv1(epochMillis: Long, sequence: Long, random: Long): UUID = {
    val Version = 0x1L
    val gregorianTimestamp = toUUIDTimestamp(epochMillis)
    val time_high =
      gregorianTimestamp >>> 48 // 12 most significant bits of the timestamp
    val time__mid =
      (gregorianTimestamp >>> 32) & 0xffff // 16 middle bits of the timestamp
    val time_low =
      gregorianTimestamp & 0xffff_ffff // 32 least significant bits of the timestamp
    val node =
      ((random << 16) >>> 16) | (0x1L << 40) // 48 bits (MAC address with the unicast bit set to 1)
    val clock_seq = sequence & 0x3fff // 14 bits
    val msb = (time_low << 32) | time__mid << 16 | (Version << 12) | time_high
    val lsb = (Variant << 62) | clock_seq << 48 | node
    new UUID(msb, lsb)
  }

  def buildUUIDv4(randomHigh: Long, randomLow: Long): UUID = {
    val Version = 0x4L
    val msb = randomHigh & ~(0xf << 12) | (Version << 12)
    val lsb = (Variant << 62) | (randomLow << 2 >>> 2)
    new UUID(msb, lsb)
  }

  def buildUUIDv6(epochMillis: Long, sequence: Long, random: Long): UUID = {
    val Version = 0x6L
    val gregorianTimestamp = toUUIDTimestamp(epochMillis)
    val time_high_and_mid =
      gregorianTimestamp >>> 12 // 48 most significant bits of the timestamp
    val time_low =
      gregorianTimestamp & 0xfff // 12 least significant bits of the timestamp
    val node = (random << 16) >>> 16 // 48 bits
    val clock_seq = sequence & 0x3fff // 14 bits
    val msb = (time_high_and_mid << 16) | (Version << 12) | time_low
    val lsb = (Variant << 62) | clock_seq << 48 | node
    new UUID(msb, lsb)
  }

  def buildUUIDV7(epochMillis: Long, sequence: Long, random: Long): UUID = {
    val Version = 0x7L
    val rand_a = sequence & 0xfffL // 12 bits
    val rand_b = (random << 2) >>> 2 // we need only 62 bits of randomness
    val msb = (epochMillis << 16) | (Version << 12) | rand_a
    val lsb = (Variant << 62) | rand_b
    new UUID(msb, lsb)
  }

  /** number of 100 nanosecond intervals since the beginning of the gregorian
    * calendar (15-oct-1582) to Unix Epoch
    */
  private val UnixEpochClockOffset = 0x01b21dd213814000L

  @inline
  final def toUUIDTimestamp(epochMillis: Long): Long = {
    val ClockMultiplier =
      10000L //  count of 100 nanosecond intervals in a milli
    val ts = epochMillis * ClockMultiplier + UnixEpochClockOffset
    (ts << 4) >>> 4 // Keeping only the 60 least significant bits
  }
}
