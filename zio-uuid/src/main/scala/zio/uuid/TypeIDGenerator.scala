package zio.uuid

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
    UUIDGenerator.uuidV7 >>> ZLayer.fromZIO {
      ZIO.serviceWith[UUIDGenerator](uuidGenerator =>
        new TypeIDGenerator {
          override def typeid(prefix: String): IO[IllegalArgumentException, TypeID] =
            uuidGenerator.uuid.flatMap { uuid =>
              TypeID
                .build(prefix, uuid)
                .mapError(errors => new IllegalArgumentException(errors.render))
                .toZIO
            }
        }
      )
    }

  /**
   * Return a UUID V7 based TypeID generator with fixed prefix.
   *
   * @param prefix
   * a string at most 63 characters in all lowercase ASCII [a-z]. Will lift
   * an error in the F context if the prefix is invalid.
   */
  def generate(prefix: String): ZIO[TypeIDGenerator, IllegalArgumentException, TypeID] =
    ZIO.serviceWithZIO[TypeIDGenerator](_.typeid(prefix))

}
