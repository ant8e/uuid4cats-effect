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

import zio.json.JsonCodec
import zio.prelude.{Debug, Equal, Validation}
import zio.uuid.internals.UUIDBase32

import java.util.UUID

/**
 * TypeIDs are a type-safe extension of UUIDv7, they encode UUIDs in base32 and
 * add a type prefix. see https://github.com/jetpack-io/typeid/tree/main/spec
 */
final case class TypeID(prefix: String, uuid: UUID) {
  def value: String = repr

  private lazy val repr = prefix + (if (prefix.nonEmpty) "_" else "") + UUIDBase32.toBase32(uuid)

  override def toString: String = s"TypeID:$repr($uuid)"
}

object TypeID {
  implicit val typeIdDebug: Debug[TypeID]       = Debug.make(_.toString)
  implicit val typeIdOrdering: Ordering[TypeID] = Ordering.by(_.value)
  implicit val typeIdEqual: Equal[TypeID]       = Equal.default

  implicit val typeIDCodec: JsonCodec[TypeID] =
    JsonCodec.string.transformOrFail[TypeID](TypeID.decode(_).toEitherWith(_.mkString(", ")), _.value)

  /**
   * Build a TypeID based on the supplied prefix and uuid.
   *
   * @param prefix
   * a string at most 63 characters in all lowercase ASCII [a-z].
   * @param enforceUUIDV7
   * true by default. When set to false, allows to build an out of spec
   * TypeID based on a other type of UUID.
   */
  def build(
    prefix: String,
    uuid: UUID,
    enforceUUIDV7: Boolean = true,
  ): Validation[BuildError, TypeID] =
    Validation
      .validateWith(
        validatePrefix(prefix),
        validateUUID(uuid, enforceUUIDV7),
      )(TypeID.apply)

  private val regex = "^([a-z]{0,63})(_?)([0123456789abcdefghjkmnpqrstvwxyz]+)$".r

  /**
   * Decode a TypeID from a string representation.
   *
   * @param enforceUUIDV7
   * true by default. When set to false, allows to decode an out of spec
   * TypeID based on a other type of UUID.
   */
  def decode(
    typeIDString: String,
    enforceUUIDV7: Boolean = true,
  ): Validation[DecodeError, TypeID] = {
    def parseID(id: String): Validation[DecodeError, UUID] =
      UUIDBase32.fromBase32(id) match {
        case Left(e)                                           => Validation.fail(DecodeError.InvalidTypeID(e))
        case Right(uuid) if enforceUUIDV7 && uuid.version != 7 => Validation.fail(DecodeError.NotUUIDV7)
        case Right(uuid)                                       => Validation.succeed(uuid)
      }

    typeIDString match {
      case regex(prefix, sep, id) =>
        if (prefix.isEmpty && sep.isEmpty || prefix.nonEmpty && sep.nonEmpty) parseID(id).map(TypeID(prefix, _))
        else Validation.fail(if (prefix.isEmpty) DecodeError.BadSeparator else DecodeError.MissingSeparator)
      case _                      => Validation.fail(DecodeError.NotParseableTypeID)
    }
  }

  private[zio] def validatePrefix(prefix: String): Validation[BuildError, String] =
    if ((prefix ne null) && "[a-z]{0,63}".r.matches(prefix)) Validation.succeed(prefix)
    else Validation.fail(BuildError.InvalidPrefix)

  private[zio] def validateUUID(uuid: UUID, enforceUUIDV7: Boolean): Validation[BuildError, UUID] =
    if ((uuid ne null) && (!enforceUUIDV7 || uuid.version() == 7)) Validation.succeed(uuid)
    else Validation.fail(BuildError.InvalidUUID)

  sealed trait BuildError extends Product with Serializable
  object BuildError {
    case object InvalidPrefix extends BuildError {
      val message = "prefix is not at most 63 characters in all lowercase ASCII [a-z]"
    }
    case object InvalidUUID   extends BuildError {
      val message = "uuid is not a UUIDv7"
    }

    implicit val buildErrorShow: Debug[BuildError] =
      Debug.make {
        case InvalidPrefix => s"InvalidPrefix: ${InvalidPrefix.message}"
        case InvalidUUID   => s"InvalidUUID: ${InvalidUUID.message}"
      }
  }

  sealed trait DecodeError extends Product with Serializable {
    def message: String
  }
  object DecodeError {
    case object NotParseableTypeID                  extends DecodeError {
      override val message = "Could not parse the supplied string as a TypeID"
    }
    case object BadSeparator                        extends DecodeError {
      override val message = "An empty prefix should not have a separator"
    }
    case object MissingSeparator                    extends DecodeError {
      override val message = "A separator was not found"
    }
    case object NotUUIDV7                           extends DecodeError {
      override val message = "The decoded UUID is not a mandatory V7"
    }
    final case class InvalidTypeID(message: String) extends DecodeError
  }
}
