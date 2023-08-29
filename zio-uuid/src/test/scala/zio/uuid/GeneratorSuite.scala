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
import zio.{URIO, ZIO}

import java.util.UUID

class GeneratorSuite extends ZSuite {

  private val n = 10000

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
    for {
      typeids <- ZIO.collectAll(List.tabulate(n)(_ => TypeIDGenerator.generate("prefix"))).provideLayer(TypeIDGenerator.live)
      _       <- ZIO.succeed(assert(typeids.distinct.size == typeids.size))
      _       <- ZIO.succeed(assert(isSeqSorted(typeids)))
    } yield ()
  }

  testZ("TypeID generator should not accept illegal prefix") {
    ZIO
      .serviceWithZIO[TypeIDGenerator](_.typeid("WRONG"))
      .provideLayer(TypeIDGenerator.live)
      .interceptFailure[IllegalArgumentException]
  }

  private def genN[R, UUIDvX](gen: URIO[R, UUIDvX], n: Int): URIO[R, List[UUIDvX]] =
    ZIO.collectAll(List.tabulate(n)(_ => gen))

  implicit class UUIDsOps(uuids: List[UUID]) {
    def allUniques: Boolean = uuids.distinct.size == uuids.size
    def isSorted: Boolean   = isSeqSorted(uuids)
  }

  private def assertAllUnique(uuids: List[UUID]) = ZIO.succeed(assert(uuids.allUniques, s"Not Unique : $uuids"))
  private def assertSorted(uuids: List[UUID])    = ZIO.succeed(assert(uuids.isSorted, s"Not sorted : ${findNotSorted(uuids)}"))

  // Using a custom ordering based on String because UUID compareTo() is not reliable
  // https://github.com/scala-js/scala-js/issues/4882 and
  // https://bugs.openjdk.org/browse/JDK-7025832
  implicit val uuidOrdering: Ordering[UUID] =
    Ordering.by[UUID, String](_.toString)

  def isSeqSorted[T](
    seq: List[T]
  )(implicit ordering: Ordering[T]): Boolean = {
    val lastIndex = seq.length - 1
    !seq.zipWithIndex.exists { case (t, index) =>
      index != lastIndex && ordering.gt(t, seq(index + 1))
    }
  }

  def findNotSorted[T](seq: List[T])(implicit ordering: Ordering[T]): String = {
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
