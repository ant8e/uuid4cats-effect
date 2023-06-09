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

import cats.effect.IO
import cats.implicits._
import munit.CatsEffectSuite

import java.util.UUID

class GeneratorSuite extends CatsEffectSuite {

  private val n = 10000
  test("UUIDv1 should generate UUIDs") {
    for {
      uuids <- UUIDv1.generator[IO].flatMap(genN(_, n))
      _ <- IO(uuids.allUniques).assert
      _ <- IO(uuids.isSorted).assert
    } yield ()
  }

  test("UUIDv4 should generate UUIDs") {
    for {
      uuids <- UUIDv4.generator[IO].flatMap(genN(_, n))
      _ <- IO(uuids.allUniques).assert
    } yield ()
  }

  test("UUIDv6 should generate UUIDs") {
    for {
      uuids <- UUIDv6.generator[IO].flatMap(genN(_, n))
      _ <- IO(uuids.allUniques).assert
      _ <- IO(uuids.isSorted).assert
    } yield ()
  }

  test("UUIDv7 should generate UUIDs") {
    for {
      uuids <- UUIDv7.generator[IO].flatMap(genN(_, n))
      _ <- IO(uuids.allUniques).assert
      _ <- IO(uuids.isSorted).assert
    } yield ()
  }

  private def genN(generator: UUIDGenerator[IO], n: Int): IO[List[UUID]] =
    List.tabulate(n)(_ => generator.uuid).sequence

  implicit class UUIDsOps(uuids: List[UUID]) {
    def allUniques: Boolean = uuids.distinct.size === uuids.size
    def isSorted: Boolean =
      uuids.sliding(2).toList.forall(l => l.head <= l.last)
  }
}
