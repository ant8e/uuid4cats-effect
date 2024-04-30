/*
 * Copyright 2023 Antoine Comte
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
import cats.syntax.all.*
import cats.~>
import tech.ant8e.uuid4cats.TimestampedUUIDGeneratorBuilder.GeneratorState

import java.util.UUID

trait UUIDGenerator[F[_]] {
  def uuid: F[UUID]
  def mapK[G[_]](fk: F ~> G): UUIDGenerator[G] = {
    new UUIDGenerator[G] {
      override def uuid: G[UUID] = fk(UUIDGenerator.this.uuid)
    }
  }
}

object UUIDGenerator {

  /** Summon an instance of UUIDGenerator for F. */
  @inline def apply[F[_]](implicit
      instance: UUIDGenerator[F]
  ): UUIDGenerator[F] = instance
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

object TimestampedUUIDGeneratorBuilder {
  private[uuid4cats] case class GeneratorState(
      lastUsedEpochMillis: Long,
      sequence: Long
  )
}

sealed trait TimestampedUUIDGeneratorBuilder {
  private type UUIDBuilder = (Long, Long, Long) => UUID
  private def generate[F[_]: Async](
      state: Ref[F, GeneratorState],
      mutex: Mutex[F],
      random: Random[F],
      builder: UUIDBuilder
  ): F[UUID] = for {
    random <- random.nextLong
    uuid <- mutex.lock.surround(
      for {
        timestamp <- Clock[F].realTime.map(_.toMillis)
        modifiedState <- state.modify { currentState =>
          // realTime clock may run backward
          val actualTimestamp =
            Math.max(currentState.lastUsedEpochMillis, timestamp)
          val sequence =
            if (currentState.lastUsedEpochMillis === actualTimestamp)
              currentState.sequence + 1
            else 0L

          val newState = GeneratorState(actualTimestamp, sequence)
          (newState, newState)
        }
      } yield builder(
        modifiedState.lastUsedEpochMillis,
        modifiedState.sequence,
        random
      )
    )
  } yield uuid

  protected def buildGenerator[F[_]: Async](
      builder: UUIDBuilder
  ): F[UUIDGenerator[F]] = {
    val generatorInitialState = Ref[F].of(GeneratorState(0L, 0L))
    for {
      state <- generatorInitialState
      mutex <- Mutex[F]
      random <- SecureRandom.javaSecuritySecureRandom[F]
    } yield new UUIDGenerator[F] {
      override def uuid: F[UUID] = generate(state, mutex, random, builder)
    }
  }
}
