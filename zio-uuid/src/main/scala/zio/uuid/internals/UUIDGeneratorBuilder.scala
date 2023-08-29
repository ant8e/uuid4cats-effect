package zio.uuid.internals

import zio.{Clock, Ref, Semaphore, UIO, ZIO}

import java.util.concurrent.TimeUnit

private[zio] final case class GeneratorState(lastUsedEpochMillis: Long, sequence: Long)

private[zio] object GeneratorState {
  val initial: GeneratorState = GeneratorState(lastUsedEpochMillis = 0L, sequence = 0L)
}

private[zio] object UUIDGeneratorBuilder {
  type UUIDBuilder[UUIDvX] = (Long, Long, Long) => UUIDvX

  def generate[UUIDvX](
    state: Ref[GeneratorState],
    mutex: Semaphore,
    random: zio.Random,
    clock: Clock,
    builder: UUIDBuilder[UUIDvX],
  ): UIO[UUIDvX] =
    for {
      modifiedState <- mutex.withPermit {
                         for {
                           timestamp     <- clock.currentTime(TimeUnit.MILLISECONDS)
                           modifiedState <- state.modify { currentState =>
                                              // currentTime clock may run backward
                                              val actualTimestamp = Math.max(currentState.lastUsedEpochMillis, timestamp)
                                              val sequence        =
                                                if (currentState.lastUsedEpochMillis == actualTimestamp) {
                                                  currentState.sequence + 1
                                                } else 0L

                                              val newState = GeneratorState(lastUsedEpochMillis = actualTimestamp, sequence = sequence)
                                              (newState, newState)
                                            }
                         } yield modifiedState
                       }
      random        <- random.nextLong
    } yield builder(
      modifiedState.lastUsedEpochMillis,
      modifiedState.sequence,
      random,
    )

  // noinspection YieldingZIOEffectInspection
  def buildGenerator[UUIDvX](builder: UUIDBuilder[UUIDvX]): UIO[UIO[UUIDvX]] =
    for {
      state  <- Ref.make(GeneratorState.initial)
      mutex  <- Semaphore.make(1)
      random <- ZIO.random
      clock  <- ZIO.clock
    } yield generate(state, mutex, random, clock, builder)
}
