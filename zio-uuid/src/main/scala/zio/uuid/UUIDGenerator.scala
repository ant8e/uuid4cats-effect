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

package zio.uuid

import zio.uuid.internals.UUIDBuilder
import zio.uuid.internals.UUIDGeneratorBuilder.buildGenerator
import zio.uuid.types.{UUIDv1, UUIDv6, UUIDv7}
import zio.{UIO, ULayer, URIO, ZIO, ZLayer}

trait UUIDGenerator {
  def uuidV1: UIO[UUIDv1]
  def uuidV6: UIO[UUIDv6]
  def uuidV7: UIO[UUIDv7]
}

final class UUIDGeneratorLive(private val v1: UIO[UUIDv1], private val v6: UIO[UUIDv6], private val v7: UIO[UUIDv7]) extends UUIDGenerator {
  override def uuidV1: UIO[UUIDv1] = v1
  override def uuidV6: UIO[UUIDv6] = v6
  override def uuidV7: UIO[UUIDv7] = v7
}

object UUIDGenerator {

  /**
   * Accessor functions
   *
   * ⚠️⚠️⚠️
   * Should not be used this way:
   * {{{
   *   val id0 = UUIDGenerator.uuidV1.provideLayer(UUIDGenerator.live)
   *   val id1 = UUIDGenerator.uuidV1.provideLayer(UUIDGenerator.live)
   * }}}
   *
   * Instead, use the following:
   * {{{
   *   (
   *     for {
   *       id0 <- UUIDGenerator.uuidV1
   *       id1 <- UUIDGenerator.uuidV1
   *     } yield ...
   *   ).provideLayer(UUIDGenerator.live)
   * }}}
   *
   * The best way to use the `UUIDGenerator` is to inject its 'live' layer in the boot sequence of your program so that the same instance
   * is reused everywhere in your program.
   *
   * If incorrectly used, the generated UUIDs are not guaranteed to be generated in a monotonic order.
   * ⚠️⚠️⚠️
   */
  val uuidV1: URIO[UUIDGenerator, UUIDv1] = ZIO.serviceWithZIO(_.uuidV1)

  /**
   * Accessor functions
   *
   * ⚠️⚠️⚠️
   * Should not be used this way:
   * {{{
   *   val id0 = UUIDGenerator.uuidV6.provideLayer(UUIDGenerator.live)
   *   val id1 = UUIDGenerator.uuidV6.provideLayer(UUIDGenerator.live)
   * }}}
   *
   * Instead, use the following:
   * {{{
   *   (
   *     for {
   *       id0 <- UUIDGenerator.uuidV6
   *       id1 <- UUIDGenerator.uuidV6
   *     } yield ...
   *   ).provideLayer(UUIDGenerator.live)
   * }}}
   *
   * The best way to use the `UUIDGenerator` is to inject its 'live' layer in the boot sequence of your program so that the same instance
   * is reused everywhere in your program.
   *
   * If incorrectly used, the generated UUIDs are not guaranteed to be generated in a monotonic order.
   * ⚠️⚠️⚠️
   */
  val uuidV6: URIO[UUIDGenerator, UUIDv6] = ZIO.serviceWithZIO(_.uuidV6)

  /**
   * Accessor functions
   *
   * ⚠️⚠️⚠️
   * Should not be used this way:
   * {{{
   *   val id0 = UUIDGenerator.uuidV7.provideLayer(UUIDGenerator.live)
   *   val id1 = UUIDGenerator.uuidV7.provideLayer(UUIDGenerator.live)
   * }}}
   *
   * Instead, use the following:
   * {{{
   *   (
   *     for {
   *       id0 <- UUIDGenerator.uuidV7
   *       id1 <- UUIDGenerator.uuidV7
   *     } yield ...
   *   ).provideLayer(UUIDGenerator.live)
   * }}}
   *
   * The best way to use the `UUIDGenerator` is to inject its 'live' layer in the boot sequence of your program so that the same instance
   * is reused everywhere in your program.
   *
   * If incorrectly used, the generated UUIDs are not guaranteed to be generated in a monotonic order.
   * ⚠️⚠️⚠️
   */
  val uuidV7: URIO[UUIDGenerator, UUIDv7] = ZIO.serviceWithZIO(_.uuidV7)

  /**
   * return a UUIDv1 (gregorian timestamp based, non-sortable) generator with
   * guarantee about the uniqueness of the UUID, even within the same
   * millisecond timestamp.
   *
   * This function uses a randomized MAC address.
   */
  val live: ULayer[UUIDGenerator] =
    ZLayer.fromZIO {
      for {
        v1 <- buildGenerator(UUIDBuilder.buildUUIDv1)
        v6 <- buildGenerator(UUIDBuilder.buildUUIDv6)
        v7 <- buildGenerator(UUIDBuilder.buildUUIDv7)
      } yield new UUIDGeneratorLive(v1, v6, v7)
    }

}
