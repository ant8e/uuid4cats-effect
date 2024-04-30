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
import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.*

import java.util.UUID

trait ExtractorSuiteBase[T] extends ScalaCheckSuite {

  def extractTimestamp(uuid: UUID): Option[T]
  def timeStampFromEpochMillis(epochMillis: Long): T

  property("UUIDv1 timestamp is correctly extracted") {
    forAll(
      Gen.posNum[Long],
      Gen.posNum[Long],
      Gen.long
    ) { (epochMillis: Long, sequence: Long, random: Long) =>
      val uuid = UUIDBuilder.buildUUIDv1(epochMillis, sequence, random)
      val maybeInstant = extractTimestamp(uuid)
      maybeInstant.contains(timeStampFromEpochMillis(epochMillis))
    }
  }

  property("UUIDv4 has no timestamp ") {
    forAll(
      Gen.long,
      Gen.long
    ) { (randomLow: Long, randomHigh: Long) =>
      val uuid = UUIDBuilder.buildUUIDv4(randomLow, randomHigh)
      val maybeInstant = extractTimestamp(uuid)
      maybeInstant.isEmpty
    }
  }

  property("UUIDv6 timestamp is correctly extracted") {
    forAll(
      Gen.posNum[Long],
      Gen.posNum[Long],
      Gen.long
    ) { (epochMillis: Long, sequence: Long, random: Long) =>
      val uuid = UUIDBuilder.buildUUIDv6(epochMillis, sequence, random)
      extractTimestamp(uuid).contains(timeStampFromEpochMillis(epochMillis))
    }
  }
  property("UUIDv7 timestamp is correctly extracted") {
    forAll(
      Gen.posNum[Long],
      Gen.posNum[Long],
      Gen.long
    ) { (epochMillis: Long, sequence: Long, random: Long) =>
      val uuid = UUIDBuilder.buildUUIDV7(epochMillis, sequence, random)
      extractTimestamp(uuid).contains(timeStampFromEpochMillis(epochMillis))
    }
  }

  property("UUIDTimestamp should round trip") {
    forAll(
      Gen.chooseNum[Long](-12219292800000L, 103072857660684L)
    ) { (ts: Long) =>
      {
        val uuidTs = UUIDBuilder.toUUIDTimestamp(ts)
        ts == UUIDBuilder.fromUUIDTimestamp(uuidTs)
      }
    }
  }
}
