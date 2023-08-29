package zio.uuid

import zio.prelude.Debug.Renderer
import zio.prelude.{DebugOps, ZValidation}
import zio.uuid.TypeID.validatePrefix
import zio.{IO, ULayer, ZIO, ZLayer}

trait TypeIDGenerator {

  /**
   * Create a new TypeID.
   *
   * @param prefix
   * a string at most 63 characters in all lowercase ASCII [a-z]. Will lift
   * an error in the F context if the prefix is invalid.
   */
  def typeid(prefix: String): IO[IllegalArgumentException, TypeID]
}

object TypeIDGenerator {

  /**
   *  Return a UUID V7 based TypeID generator
   */
  val live: ULayer[TypeIDGenerator] =
    UUIDGenerator.live >>>
      ZLayer.fromZIO {
        ZIO.serviceWith[UUIDGenerator](new TypeIDGeneratorLive(_))
      }

  /**
   * Accessor function
   *
   * ⚠️⚠️⚠️
   * Should not be used this way:
   * {{{
   *   val id0 = TypeIDGenerator.generate("prefix").provideLayer(TypeIDGenerator.live)
   *   val id1 = TypeIDGenerator.generate("prefix").provideLayer(TypeIDGenerator.live)
   * }}}
   *
   * Instead, use the following:
   * {{{
   *   (
   *     for {
   *       id0 <- TypeIDGenerator.generate("prefix")
   *       id1 <- TypeIDGenerator.generate("prefix")
   *     } yield ...
   *   ).provideLayer(TypeIDGenerator.live)
   * }}}
   *
   * The best way to use the `TypeIDGenerator` is to inject its 'live' layer in the boot sequence of your program so that the same instance
   * is reused everywhere in your program.
   *
   * If incorrectly used, the generated TypedIDs are not guaranteed to be generated in a monotonic order.
   * ⚠️⚠️⚠️
   *
   * @param prefix
   * a string at most 63 characters in all lowercase ASCII [a-z]. Will lift
   * an error in the F context if the prefix is invalid.
   */
  def generate(prefix: String): ZIO[TypeIDGenerator, IllegalArgumentException, TypeID] =
    ZIO.serviceWithZIO[TypeIDGenerator](_.typeid(prefix))

}

final class TypeIDGeneratorLive(uuidGenerator: UUIDGenerator) extends TypeIDGenerator {
  override def typeid(prefix: String): IO[IllegalArgumentException, TypeID] =
    ZIO.suspendSucceed {
      validatePrefix(prefix) match {
        case ZValidation.Success(_, _)      => uuidGenerator.uuidV7.map(TypeID(prefix, _))
        case ZValidation.Failure(_, errors) => ZIO.fail(new IllegalArgumentException(errors.debug.render(Renderer.Scala)))
      }
    }
}
