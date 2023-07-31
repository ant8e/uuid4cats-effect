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

import cats.effect.IO
import cats.syntax.all._
import munit.CatsEffectSuite

import java.util.UUID

class GeneratorSuite extends CatsEffectSuite {

  private val n = 10000
  test("UUIDv1 should generate UUIDs") {
    for {
      uuids <- UUIDv1.generator[IO].flatMap(genN(_, n))
      _ <- assertAllUnique(uuids)
    } yield ()
  }

  test("UUIDv4 should generate UUIDs") {
    for {
      uuids <- UUIDv4.generator[IO].flatMap(genN(_, n))
      _ <- assertAllUnique(uuids)
    } yield ()
  }

  test("UUIDv6 should generate UUIDs") {
    for {
      uuids <- UUIDv6.generator[IO].flatMap(genN(_, n))
      _ <- assertAllUnique(uuids)
      _ <- assertSorted(uuids)
    } yield ()
  }

  test("UUIDv7 should generate UUIDs") {
    for {
      uuids <- UUIDv7.generator[IO].flatMap(genN(_, n))
      _ <- assertAllUnique(uuids)
      _ <- assertSorted(uuids)
    } yield ()
  }

  test("TypeID should generate TypeIds") {
    for {
      typeids <- TypeID
        .generator[IO]
        .flatMap(generator =>
          List.tabulate(n)(_ => generator.typeid("prefix")).sequence
        )
      _ <- IO(typeids.distinct.size === typeids.size).assert
      _ <- IO(isSeqSorted(typeids)).assert
    } yield ()

    for {
      typeids <- TypeID
        .generator[IO]("prefix")
        .flatMap(generator => List.tabulate(n)(_ => generator()).sequence)
      _ <- IO(typeids.distinct.size === typeids.size).assert
      _ <- IO(isSeqSorted(typeids)).assert
    } yield ()
  }

  test("TypeID generator should not accept illegal prefix") {
    TypeID
      .generator[IO]
      .flatMap(generator => generator.typeid("WRONG"))
      .intercept[IllegalArgumentException]

    TypeID.generator[IO]("WRONG").intercept[IllegalArgumentException]
  }

  private def genN(generator: UUIDGenerator[IO], n: Int): IO[List[UUID]] =
    List.tabulate(n)(_ => generator.uuid).sequence

  implicit class UUIDsOps(uuids: List[UUID]) {
    def allUniques: Boolean = uuids.distinct.size === uuids.size
    def isSorted: Boolean = isSeqSorted(uuids)
  }

  private def assertAllUnique(uuids: List[UUID]) = {
    assertIOBoolean(uuids.allUniques.pure[IO], s"Not Unique : $uuids")
  }

  private def assertSorted(uuids: List[UUID]) = {
    assertIOBoolean(
      uuids.isSorted.pure[IO],
      s"Not sorted : ${findNotSorted(uuids)}"
    )
  }

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
