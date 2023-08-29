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
import munit.FunSuite
import zio.uuid.types.{UUIDv1, UUIDv6, UUIDv7}

import java.util.UUID

class BuilderSuite extends FunSuite {

  test("UUIDBuilder should correctly build UUIDv1") {
    // Differ by one bit of the test value in the RFC as we force the unicast bit to 1
    val expected = UUIDv1.unsafe(uuid"C232AB00-9414-11EC-B3C8-9F6BDECED846")

    val obtained =
      UUIDBuilder.buildUUIDv1(1645557742000L, 0x33c8L, 0x9e6bdeced846L)
    assertEquals(obtained, expected)
  }

  test("UUIDBuilder should correctly build UUIDv6") {
    val expected = UUIDv6.unsafe(uuid"1EC9414C-232A-6B00-B3C8-9E6BDECED846")
    val obtained =
      UUIDBuilder.buildUUIDv6(1645557742000L, 0x33c8L, 0x9e6bdeced846L)
    assertEquals(obtained, expected)
  }

  test("UUIDBuilder should correctly build UUIDv7") {
    val expected = UUIDv7.unsafe(uuid"017F22E2-79B0-7CC3-98C4-DC0C0C07398F")
    val obtained =
      UUIDBuilder.buildUUIDv7(0x17f22e279b0L, 0xcc3L, 0x18c4dc0c0c07398fL)
    assertEquals(obtained, expected)
  }

  implicit class uuidOps(sc: StringContext) {
    def uuid(args: Any*): UUID = UUID.fromString(sc.s(args: _*))
  }
}
