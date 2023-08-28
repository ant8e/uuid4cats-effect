---
id: index
title: "Getting Started with zio-uuid"
sidebar_label: "Getting Started"
---

[ZIO UUID](https://github.com/guizmaii/zio-uuid) is a "ZIOfied" fork
of [uuid4cats-effect](https://github.com/ant8e/uuid4cats-effect) by [Antoine Comte](https://github.com/ant8e)

@PROJECT_BADGES@

## Introduction

This library add support for the following types:

|         |         time-based         | sortable | random |
|--------:|:--------------------------:|:--------:|:------:|
| UUID v1 | ✅ <br/> gregorian calendar |          |        |
| UUID v6 | ✅ <br/> gregorian calendar |    ✅     |        |
| UUID v7 |     ✅ <br/>unix epoch      |    ✅     |   ✅    |

Implementation based on this [UUID RFC Draft](https://datatracker.ietf.org/doc/html/draft-ietf-uuidrev-rfc4122bis-03)

In addition to UUID, there is also support for [TypeIDs](https://github.com/jetpack-io/typeid). TypeIDs are a modern,
type-safe extension of UUIDv7

_ZIO implementation note:_ Note, that we don't provide a UUIDv4 implementation in this lib. ZIO is already providing one
with `ZIO.randomWith(_.nextUUID)`

## Installation

In order to use this library, we need to add the following line in our `build.sbt` file:

```scala
libraryDependencies += "dev.zio" %% "zio-uuid" % "@VERSION@"
```

## Example

```scala
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import zio.uuid.UUIDv6
import zio.uuid.TypeID

val ids =
  (
    for {
      uuid1 <- UUIDGenerator.uuid
      uuid2 <- UUIDGenerator.uuid
      typeid <- TypeIDGenerator.generate("myprefix")
    } yield (uuid1, uuid2, typeid.value)
  ).provideLayers(UUIDGenerator.uuidV7, TypeIDGenerator.live)
```

Uniqueness of generated time-based UUIDs is guaranteed when using the same generator.
Collisions across generators are theoretically possible although unlikely.


