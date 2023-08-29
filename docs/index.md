---
id: index
title: "Getting Started with zio-uuid"
sidebar_label: "Getting Started"
---

@PROJECT_BADGES@

[zio-uuid](https://github.com/guizmaii-opensource/zio-uuid) is a "ZIOfied" fork
of [uuid4cats-effect](https://github.com/ant8e/uuid4cats-effect) by [Antoine Comte](https://github.com/ant8e)

## Introduction

This library adds support for the following types:

|         |         time-based         | sortable | random |
|--------:|:--------------------------:|:--------:|:------:|
| UUID v1 | ✅ <br/> gregorian calendar |          |        |
| UUID v6 | ✅ <br/> gregorian calendar |    ✅     |        |
| UUID v7 |     ✅ <br/>unix epoch      |    ✅     |   ✅    |

Implementation based on this [UUID RFC Draft](https://datatracker.ietf.org/doc/html/draft-ietf-uuidrev-rfc4122bis-03)

In addition to UUID, there is also support for [TypeIDs](https://github.com/jetpack-io/typeid). TypeIDs are a modern,
type-safe extension of UUIDv7

_ZIO implementation note:_    
Note, that we don't provide a UUIDv4 implementation in this lib. ZIO is already providing one
with `ZIO.randomWith(_.nextUUID)`

## Installation

In order to use this library, we need to add the following line in our `build.sbt` file:

```scala
libraryDependencies += "com.guizmaii" %% "zio-uuid" % "@VERSION@"
```

## Example

```scala
import zio.uuid.*

val ids =
  (
    for {
      uuid1 <- UUIDGenerator.uuidV7
      uuid2 <- UUIDGenerator.uuidV7
      typeid <- TypeIDGenerator.generate("myprefix")
    } yield (uuid1, uuid2, typeid.value)
  ).provideLayers(UUIDGenerator.live, TypeIDGenerator.live)
```

## ⚠️ Warnings ⚠️

Uniqueness of generated time-based UUIDs is guaranteed when using the same generator.

The generators are stateful! They are using a `Ref` internally to keep track of their internal state.

The `UUIDGenerator` and `TypeIDGenerator` companion object are providing accessor functions to ease their usage but, because the generators are stateful,
the way the generator instance is provided to these functions calls can lead to generated UUIDs/TypeIDs being invalid regarding the RFC.

Do not do this:
```scala
val id0 = UUIDGenerator.uuidV7.provideLayer(UUIDGenerator.live)
val id1 = UUIDGenerator.uuidV7.provideLayer(UUIDGenerator.live)
```
This will lead to non-monotonically increasing UUIDs/TypeIDs, which is invalid regarding the RFCs.

Do this instead:
```scala
(
  for {
    id0 <- UUIDGenerator.uuidV7
    id1 <- UUIDGenerator.uuidV7
    // ...
  } yield ()
).provideLayer(UUIDGenerator.live)
```

The best way to inject a `UUIDGenerator` or a `TypeIDGenerator` instance is to inject its `live` layer in the boot sequence of your program 
so that the same instance is reused everywhere in your program and you don't risk any issue.