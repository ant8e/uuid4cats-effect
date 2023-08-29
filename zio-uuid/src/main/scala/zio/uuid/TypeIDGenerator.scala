package zio.uuid

import zio.prelude.Debug.Renderer
import zio.prelude.DebugOps
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
    UUIDGenerator.live >>> ZLayer.fromZIO {
      ZIO.serviceWith[UUIDGenerator](uuidGenerator =>
        new TypeIDGenerator {
          override def typeid(prefix: String): IO[IllegalArgumentException, TypeID] =
            uuidGenerator.uuidV7.flatMap { uuid =>
              TypeID
                .build(prefix, uuid)
                .mapError(errors => new IllegalArgumentException(errors.debug.render(Renderer.Scala)))
                .toZIO
            }
        }
      )
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
