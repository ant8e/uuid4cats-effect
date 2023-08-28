package zio.uuid.internals

import zio.{Ref, Semaphore, UIO, ZIO}

import java.util.UUID
import java.util.concurrent.TimeUnit

private[zio] final case class GeneratorState(lastUsedEpochMillis: Long, sequence: Long)
private[zio] object GeneratorState {
  val initial: GeneratorState = GeneratorState(lastUsedEpochMillis = 0L, sequence = 0L)
}

private[zio] object UUIDGeneratorBuilder {
  type UUIDBuilder = (Long, Long, Long) => UUID

  def generate(
    state: Ref[GeneratorState],
    mutex: Semaphore,
    builder: UUIDBuilder,
  ): UIO[UUID] =
    for {
      random <- ZIO.random.flatMap(_.nextLong)
      uuid   <- mutex.withPermit {
                  for {
                    timestamp     <- ZIO.clockWith(_.currentTime(TimeUnit.MILLISECONDS))
                    modifiedState <- state.modify { currentState =>
                                       // realTime clock may run backward
                                       val actualTimestamp = Math.max(currentState.lastUsedEpochMillis, timestamp)
                                       val sequence        =
                                         if (currentState.lastUsedEpochMillis == actualTimestamp) {
                                           currentState.sequence + 1
                                         } else 0L

                                       val newState = GeneratorState(lastUsedEpochMillis = actualTimestamp, sequence = sequence)
                                       (newState, newState)
                                     }
                  } yield builder(
                    modifiedState.lastUsedEpochMillis,
                    modifiedState.sequence,
                    random,
                  )
                }
    } yield uuid

  // noinspection YieldingZIOEffectInspection
  def buildGenerator(builder: UUIDBuilder): UIO[UIO[UUID]] =
    for {
      state <- Ref.make(GeneratorState.initial)
      mutex <- Semaphore.make(1)
    } yield generate(state, mutex, builder)
}
