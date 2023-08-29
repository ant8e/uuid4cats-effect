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

import munit.ZSuite
import zio.uuid.types.{UUIDv1, UUIDv6, UUIDv7}
import zio.ZIO

import java.util.UUID

class GeneratorSuite extends ZSuite {

  private val n = 2_000_000

  testZ("UUIDv1 should generate UUIDs") {
    (
      for {
        uuids <- genN(UUIDGenerator.uuidV1, n)
        _     <- assertAllUnique(uuids)
      } yield ()
    ).provideLayer(UUIDGenerator.live)
  }

  testZ("UUIDv6 should generate UUIDs") {
    (
      for {
        uuids <- genN(UUIDGenerator.uuidV6, n)
        _     <- assertAllUnique(uuids)
        _     <- assertSorted(uuids)
      } yield ()
    ).provideLayer(UUIDGenerator.live)
  }

  testZ("UUIDv7 should generate UUIDs") {
    (
      for {
        uuids <- genN(UUIDGenerator.uuidV7, n)
        _     <- assertAllUnique(uuids)
        _     <- assertSorted(uuids)
      } yield ()
    ).provideLayer(UUIDGenerator.live)
  }

  testZ("TypeID should generate TypeIds") {
    (
      for {
        typeids <- genN(TypeIDGenerator.generate("prefix"), n)
        _       <- ZIO.succeed(assert(typeids.distinct.length == typeids.length))
        _       <- ZIO.succeed(assert(isSeqSorted(typeids)))
      } yield ()
    ).provideLayer(TypeIDGenerator.live)
  }

  testZ("TypeID generator should not accept illegal prefix") {
    ZIO
      .serviceWithZIO[TypeIDGenerator](_.typeid("WRONG"))
      .provideLayer(TypeIDGenerator.live)
      .interceptFailure[IllegalArgumentException]
  }

  private def genN[R, E, UUIDvX](gen: ZIO[R, E, UUIDvX], n: Int): ZIO[R, E, Vector[UUIDvX]] =
    ZIO.replicateZIO(n)(gen).map(Vector.from)

  private def assertAllUnique[UUIDvX](uuids: Vector[UUIDvX])        =
    ZIO.succeed(assert(uuids.distinct.length == uuids.length, s"Not Unique : $uuids"))
  private def assertSorted[UUIDvX: Ordering](uuids: Vector[UUIDvX]) =
    ZIO.succeed(assert(isSeqSorted[UUIDvX](uuids), s"Not sorted : ${findNotSorted(uuids)}"))

  // Using a custom ordering based on String because UUID compareTo() is not reliable
  // https://github.com/scala-js/scala-js/issues/4882 and
  // https://bugs.openjdk.org/browse/JDK-7025832
  implicit val uuidOrdering: Ordering[UUID] =
    Ordering.by[UUID, String](_.toString)

  implicit val uuidv1Ordering: Ordering[UUIDv1] = UUIDv1.wrapAll(uuidOrdering)
  implicit val uuidv6Ordering: Ordering[UUIDv6] = UUIDv6.wrapAll(uuidOrdering)
  implicit val uuidv7Ordering: Ordering[UUIDv7] = UUIDv7.wrapAll(uuidOrdering)

  def isSeqSorted[UUIDvX](
    seq: Vector[UUIDvX]
  )(implicit ordering: Ordering[UUIDvX]): Boolean = {
    val lastIndex = seq.length - 1
    !seq.zipWithIndex.exists { case (t, index) =>
      index != lastIndex && ordering.gt(t, seq(index + 1))
    }
  }

  def findNotSorted[T](seq: Vector[T])(implicit ordering: Ordering[T]): String = {
    val lastIndex = seq.length - 1
    seq.zipWithIndex
      .find { case (t, index) =>
        index != lastIndex && ordering.gt(t, seq(index + 1))
      }
      .map { case (t, index) =>
        s"found non sorted values $t ${seq(index + 1)}} at index $index"
      }
      .getOrElse("No non sorted values found")
  }

}
