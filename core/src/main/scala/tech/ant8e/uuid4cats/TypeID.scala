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

import cats.data.{Validated, ValidatedNec}
import cats.effect.Async
import cats.syntax.all._
import cats.{ApplicativeError, Eq, Order, Show}
import tech.ant8e.uuid4cats.TypeID.BuildError.{InvalidPrefix, InvalidUUID}
import tech.ant8e.uuid4cats.TypeID.DecodeError._

import java.util.UUID
import scala.annotation.switch
import scala.util.{Failure, Success, Try}

/** TypeIDs are a type-safe extension of UUIDv7, they encode UUIDs in base32 and
  * add a type prefix. see https://github.com/jetpack-io/typeid/tree/main/spec
  */
trait TypeID {
  def prefix: String
  def uuid: UUID
  def value: String
}

object TypeID {

  trait TypeIDGenerator[F[_]] {

    /** Create a new TypeID.
      *
      * @param prefix
      *   a string at most 63 characters in all lowercase ASCII [a-z]. Will lift
      *   an error in the F context if the prefix is invalid.
      */
    def typeid(prefix: String): F[TypeID]
  }

  /** Return a UUID V7 based TypeID generator. */
  def generator[F[_]: Async]: F[TypeIDGenerator[F]] = UUIDv7
    .generator[F]
    .map(uuidGenerator =>
      (prefix: String) =>
        uuidGenerator.uuid.flatMap { uuid =>
          val typeID = build(prefix, uuid).leftMap(errors =>
            new IllegalArgumentException(errors.mkString_(", "))
          )
          ApplicativeError[F, Throwable].fromValidated(typeID)
        }
    )

  /** Return a UUID V7 based TypeID generator with fixed prefix.
    *
    * @param prefix
    *   a string at most 63 characters in all lowercase ASCII [a-z]. Will lift
    *   an error in the F context if the prefix is invalid.
    */
  def generator[F[_]: Async](prefix: String): F[() => F[TypeID]] =
    validatePrefix(prefix)
      .map(validatedPrefix =>
        generator[F].map { generator => () =>
          generator.typeid(validatedPrefix)
        }
      )
      .valueOr(error =>
        ApplicativeError[F, Throwable].raiseError(
          new IllegalArgumentException(show"$error")
        )
      )

  /** Build a TypeID based on the supplied prefix and uuid.
    *
    * @param prefix
    *   a string at most 63 characters in all lowercase ASCII [a-z].
    * @param enforceUUIDV7
    *   true by default. When set to false, allows to build an out of spec
    *   TypeID based on a other type of UUID.
    */
  def build(
      prefix: String,
      uuid: UUID,
      enforceUUIDV7: Boolean = true
  ): ValidatedNec[BuildError, TypeID] =
    (
      validatePrefix(prefix).toValidatedNec,
      validateUUID(uuid, enforceUUIDV7).toValidatedNec
    )
      .mapN { case (prefix_, uuid_) => newTypeID(prefix_, uuid_) }

  /** Decode a TypeID from a string representation.
    *
    * @param enforceUUIDV7
    *   true by default. When set to false, allows to decode an out of spec
    *   TypeID based on a other type of UUID.
    */
  def decode(
      typeIDString: String,
      enforceUUIDV7: Boolean = true
  ): ValidatedNec[DecodeError, TypeID] = {
    val re =
      "^([a-z]{0,63})(_?)([0123456789abcdefghjkmnpqrstvwxyz]+)$".r

    def parseID(id: String): Validated[DecodeError, UUID] =
      UUIDBase32
        .fromBase32(id)
        .leftMap(InvalidTypeID.apply)
        .toValidated
        .andThen {
          case uuid if enforceUUIDV7 && uuid.version != 7 =>
            NotUUIDV7.invalid[UUID].leftWiden[DecodeError]
          case uuid => uuid.valid
        }

    typeIDString match {
      case re(prefix, sep, id) =>
        (
          Validated
            .cond(
              prefix.isEmpty && sep.isEmpty || prefix.nonEmpty && sep.nonEmpty,
              prefix,
              if (prefix.isEmpty) BadSeparator else MissingSeparator
            )
            .toValidatedNec,
          parseID(id).toValidatedNec
        ).mapN { case (prefix_, uuid_) => newTypeID(prefix_, uuid_) }
      case _ => Validated.invalidNec(NotParseableTypeID)
    }

  }

  implicit val typeIDShow: Show[TypeID] = Show.show(_.toString)
  implicit val typeIDOrd: Order[TypeID] = Order.by(_.value)
  implicit val typeIDOrdering: Ordering[TypeID] = Ordering.by(_.value)
  implicit val typeIDEq: Eq[TypeID] = Eq.instance { case (a, b) =>
    a.prefix === b.prefix && a.uuid === b.uuid
  }

  sealed trait BuildError
  object BuildError {
    case object InvalidPrefix extends BuildError {
      val message =
        "prefix is not at most 63 characters in all lowercase ASCII [a-z]"
    }

    case object InvalidUUID extends BuildError {
      val message = "uuid is not a UUIDv7"
    }

    implicit val buildErrorShow: Show[BuildError] = Show.show {
      case InvalidPrefix => s"InvalidPrefix: ${InvalidPrefix.message}"
      case InvalidUUID   => s"InvalidUUID: ${InvalidUUID.message}"
    }
  }

  sealed trait DecodeError {
    def message: String
  }

  object DecodeError {
    case object NotParseableTypeID extends DecodeError {
      val message = "Could not parse the supplied string as a TypeID"
    }

    case object BadSeparator extends DecodeError {
      val message = "An empty prefix should not have a separator"
    }

    case object MissingSeparator extends DecodeError {
      val message = "A separator was not found"
    }

    case object NotUUIDV7 extends DecodeError {
      val message = "The decoded UUID is not a mandatory V7"
    }

    case class InvalidTypeID(message: String) extends DecodeError
  }

  private def newTypeID(prefix_ : String, uuid_ : UUID): TypeID = new TypeID {
    override val prefix: String = prefix_
    override val uuid: UUID = uuid_
    override def value: String = repr
    private lazy val repr =
      prefix_ + (if (prefix_.nonEmpty) "_" else "") + UUIDBase32.toBase32(uuid_)
    override def toString: String = s"TypeID:$repr($uuid_)"

    override def equals(obj: Any): Boolean = obj match {
      case other: TypeID =>
        this.uuid.equals(other.uuid) && this.prefix.equals(other.prefix)
      case _ => false
    }

  }

  private[uuid4cats] def validatePrefix(
      prefix: String
  ): Validated[BuildError, String] = Option(prefix)
    .map(prefix_ => {
      if ("[a-z]{0,63}".r.matches(prefix_))
        Validated.Valid(prefix_)
      else Validated.Invalid(InvalidPrefix)
    })
    .getOrElse(Validated.Invalid(InvalidPrefix))

  private[uuid4cats] def validateUUID(
      uuid: UUID,
      enforceUUIDV7: Boolean
  ): Validated[BuildError, UUID] = Option(uuid)
    .map(uuid => {
      if (!enforceUUIDV7 || uuid.version() === 7)
        Validated.Valid(uuid)
      else Validated.Invalid(InvalidUUID)
    })
    .getOrElse(Validated.Invalid(InvalidUUID))

  private object UUIDBase32 {
    private val encodingTable: Array[Char] =
      "0123456789abcdefghjkmnpqrstvwxyz".toArray

    def toBase32(uuid: UUID): String = {
      @inline def enc(i: Int): Char = encodingTable(i)

      val b0 = ((uuid.getMostSignificantBits >>> 56) & 0xff).toInt
      val b1 = ((uuid.getMostSignificantBits >>> 48) & 0xff).toInt
      val b2 = ((uuid.getMostSignificantBits >>> 40) & 0xff).toInt
      val b3 = ((uuid.getMostSignificantBits >>> 32) & 0xff).toInt
      val b4 = ((uuid.getMostSignificantBits >>> 24) & 0xff).toInt
      val b5 = ((uuid.getMostSignificantBits >>> 16) & 0xff).toInt
      val b6 = ((uuid.getMostSignificantBits >>> 8) & 0xff).toInt
      val b7 = (uuid.getMostSignificantBits & 0xff).toInt

      val b8 = (uuid.getLeastSignificantBits >>> 56 & 0xff).toInt
      val b9 = (uuid.getLeastSignificantBits >>> 48 & 0xff).toInt
      val b10 = (uuid.getLeastSignificantBits >>> 40 & 0xff).toInt
      val b11 = (uuid.getLeastSignificantBits >>> 32 & 0xff).toInt
      val b12 = (uuid.getLeastSignificantBits >>> 24 & 0xff).toInt
      val b13 = (uuid.getLeastSignificantBits >>> 16 & 0xff).toInt
      val b14 = (uuid.getLeastSignificantBits >>> 8 & 0xff).toInt
      val b15 = (uuid.getLeastSignificantBits & 0xff).toInt

      val out = new Array[Char](26)
      out(0) = enc(ms3b(b0))
      out(1) = enc(ls5b(b0))
      out(2) = enc(ms5b(b1))
      out(3) = enc(ls3b(b1) << 2 | ms2b(b2))
      out(4) = enc(ls5b(b2 >> 1))
      out(5) = enc(ls1b(b2) << 4 | ms4b(b3))
      out(6) = enc(ls4b(b3) << 1 | ms1b(b4))
      out(7) = enc(ls5b(b4 >> 2))
      out(8) = enc(ls2b(b4) << 3 | ms3b(b5))
      out(9) = enc(ls5b(b5))
      out(10) = enc(ms5b(b6))
      out(11) = enc(ls3b(b6) << 2 | ms2b(b7))
      out(12) = enc(ls5b(b7 >> 1))
      out(13) = enc(ls1b(b7) << 4 | ms4b(b8))
      out(14) = enc(ls4b(b8) << 1 | ms1b(b9))
      out(15) = enc(ls5b(b9 >> 2))
      out(16) = enc(ls2b(b9) << 3 | ms3b(b10))
      out(17) = enc(ls5b(b10))
      out(18) = enc(ms5b(b11))
      out(19) = enc(ls3b(b11) << 2 | ms2b(b12))
      out(20) = enc(ls5b(b12 >> 1))
      out(21) = enc(ls1b(b12) << 4 | ms4b(b13))
      out(22) = enc(ls4b(b13) << 1 | ms1b(b14))
      out(23) = enc(ls5b(b14 >> 2))
      out(24) = enc(ls2b(b14) << 3 | ms3b(b15))
      out(25) = enc(ls5b(b15))

      PlatformSpecificMethods.stringFromArray(out)
    }

    def fromBase32(s: String): Either[String, UUID] = {
      val values = Try(
        s.map(v =>
          (v: @switch) match {
            case '0' => 0L
            case '1' => 1L
            case '2' => 2L
            case '3' => 3L
            case '4' => 4L
            case '5' => 5L
            case '6' => 6L
            case '7' => 7L
            case '8' => 8L
            case '9' => 9L
            case 'a' => 10L
            case 'b' => 11L
            case 'c' => 12L
            case 'd' => 13L
            case 'e' => 14L
            case 'f' => 15L
            case 'g' => 16L
            case 'h' => 17L
            case 'j' => 18L
            case 'k' => 19L
            case 'm' => 20L
            case 'n' => 21L
            case 'p' => 22L
            case 'q' => 23L
            case 'r' => 24L
            case 's' => 25L
            case 't' => 26L
            case 'v' => 27L
            case 'w' => 28L
            case 'x' => 29L
            case 'y' => 30L
            case 'z' => 31L
          }
        )
      )

      values match {
        case Failure(_) =>
          "String representation contains at least an invalid characters".asLeft
        case Success(values) if (values.length != 26) =>
          "String representation should be exactly 26 significant characters".asLeft
        case Success(values) if (values(0) > 7) =>
          "The String representation encodes more than 128 bits".asLeft
        case Success(values) =>
              // format: off
              val msb = (values(0) << 61) | // We have only 3 significant bits at pos. 0 because of the padding
                (values(1) << 56) | (values(2) << 51) | (values(3) << 46) |
                (values(4) << 41) | (values(5) << 36) | (values(6) << 31) | (values(7) << 26) |
                (values(8) << 21) | (values(9) << 16) | (values(10) << 11) | (values(11) << 6) |
                (values(12) << 1) | ls1b((values(13) >> 4).toInt)

              val lsb = (values(13) << 60) |
                (values(14) << 55) | (values(15) << 50) | (values(16) << 45) | (values(17) << 40) |
                (values(18) << 35) | (values(19) << 30) | (values(20) << 25) | (values(21) << 20) |
                (values(22) << 15) | (values(23) << 10) | (values(24) << 5) | values(25)
              // format: on

          new UUID(msb, lsb).asRight
      }
    }

    private val Mask1Bits: Byte = 0x01
    private val Mask2Bits: Byte = 0x03
    private val Mask3Bits: Byte = 0x07
    private val Mask4Bits: Byte = 0x0f
    private val Mask5Bits: Byte = 0x1f

    @inline
    private def ls1b(b: Int) = {
      b & Mask1Bits
    }

    @inline
    private def ls2b(b: Int) = {
      b & Mask2Bits
    }

    @inline
    private def ls3b(b: Int) = {
      b & Mask3Bits
    }

    @inline
    private def ls4b(b: Int) = {
      b & Mask4Bits
    }

    @inline
    private def ls5b(b: Int) = {
      b & Mask5Bits
    }

    @inline
    private def ms5b(b: Int) = {
      (b >> 3) & Mask5Bits
    }

    @inline
    private def ms4b(b: Int) = {
      (b >> 4) & Mask4Bits
    }

    @inline
    private def ms3b(b: Int) = {
      (b >> 5) & Mask3Bits
    }

    @inline
    private def ms2b(b: Int) = {
      (b >> 6) & Mask2Bits
    }

    @inline
    private def ms1b(b: Int) = {
      (b >> 7) & Mask1Bits
    }
  }
}
