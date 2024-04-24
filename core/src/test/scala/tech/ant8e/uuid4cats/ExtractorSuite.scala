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
import org.scalacheck.Prop.*
import munit.ScalaCheckSuite
import org.scalacheck.Gen

import java.time.Instant

class ExtractorSuite extends ScalaCheckSuite {

  val instantGen: Gen[Instant] = Gen.posNum[Long].map(Instant.ofEpochMilli)

  property("UUIDv1 timestamp is correctly extracted") {
    forAll(
      instantGen,
      Gen.posNum[Long],
      Gen.long
    ) { (instant: Instant, sequence: Long, random: Long) =>
      val uuid = UUIDBuilder.buildUUIDv1(instant.toEpochMilli, sequence, random)
      val maybeInstant = UUIDUtil.extractTimestamp(uuid)
      maybeInstant.contains(instant)
    }
  }

  property("UUIDv4 has no timestamp ") {
    forAll(
      Gen.long,
      Gen.long
    ) { (randomLow: Long, randomHigh: Long) =>
      val uuid = UUIDBuilder.buildUUIDv4(randomLow, randomHigh)
      val maybeInstant = UUIDUtil.extractTimestamp(uuid)
      maybeInstant.isEmpty
    }
  }

  property("UUIDv6 timestamp is correctly extracted") {
    forAll(
      instantGen,
      Gen.posNum[Long],
      Gen.long
    ) { (instant: Instant, sequence: Long, random: Long) =>
      val uuid = UUIDBuilder.buildUUIDv6(instant.toEpochMilli, sequence, random)
      UUIDUtil.extractTimestamp(uuid).contains(instant)
    }
  }
  property("UUIDv7 timestamp is correctly extracted") {
    forAll(
      instantGen,
      Gen.posNum[Long],
      Gen.long
    ) { (instant: Instant, sequence: Long, random: Long) =>
      val uuid = UUIDBuilder.buildUUIDV7(instant.toEpochMilli, sequence, random)
      UUIDUtil.extractTimestamp(uuid).contains(instant)
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
