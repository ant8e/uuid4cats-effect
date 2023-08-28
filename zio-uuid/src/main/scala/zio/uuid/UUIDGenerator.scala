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
import zio.{UIO, ULayer, URIO, ZIO, ZLayer}

import java.util.UUID

trait UUIDGenerator {
  def uuid: UIO[UUID]
}

final class UUIDv1Generator(private val generate: UIO[UUID]) extends UUIDGenerator {
  override def uuid: UIO[UUID] = generate
}

final class UUIDv6Generator(private val generate: UIO[UUID]) extends UUIDGenerator {
  override def uuid: UIO[UUID] = generate
}

final class UUIDv7Generator(private val generate: UIO[UUID]) extends UUIDGenerator {
  override def uuid: UIO[UUID] = generate
}

object UUIDGenerator {

  /**
   * Accessor function
   */
  val uuid: URIO[UUIDGenerator, UUID] = ZIO.serviceWithZIO(_.uuid)

  /**
   * return a UUIDv1 (gregorian timestamp based, non-sortable) generator with
   * guarantee about the uniqueness of the UUID, even within the same
   * millisecond timestamp.
   *
   * This function uses a randomized MAC address.
   */
  val uuidV1: ULayer[UUIDGenerator] =
    ZLayer.fromZIO {
      buildGenerator(UUIDBuilder.buildUUIDv1).map(new UUIDv1Generator(_))
    }

  /**
   * return a UUIDv6 (gregorian timestamp based, sortable) generator with
   * guarantee about the uniqueness of the UUID, even within the same
   * millisecond timestamp.
   */
  val uuidV6: ULayer[UUIDGenerator] =
    ZLayer.fromZIO {
      buildGenerator(UUIDBuilder.buildUUIDv6).map(new UUIDv6Generator(_))
    }

  /**
   * return a UUIDv7 (unix epoch timestamp based, sortable) generator with
   * guarantee about the uniqueness of the UUID, even within the same
   * millisecond timestamp.
   */
  val uuidV7: ULayer[UUIDGenerator] =
    ZLayer.fromZIO {
      buildGenerator(UUIDBuilder.buildUUIDV7).map(new UUIDv7Generator(_))
    }
}
