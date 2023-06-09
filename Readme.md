
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/tech.ant8e/uuid4cats-effect_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/tech.ant8e/uuid4cats-effect_2.13)
[![Code of Conduct](https://img.shields.io/badge/Code%20of%20Conduct-Scala-blue.svg)](CODE_OF_CONDUCT.md)
![](https://github.com/ant8e/uuid4cats-effect/actions/workflows/ci.yml/badge.svg)
# uuid4cats-effect -  UUID Generation for cats effect


Although cats-effect has some support for [generating UUIDs](https://typelevel.org/cats-effect/api/3.x/cats/effect/std/UUIDGen.html), it is limited to the UUIDv4 pseudo-random type.
                                                                                 

This library add support for the following types:

|         |         time-based         | sortable | random |
|--------:|:--------------------------:|:--------:|:------:|
| UUID v1 | ✅ <br/> gregorian calendar |          |        |
| UUID v4 |                            |          |   ✅    |
| UUID v6 | ✅ <br/> gregorian calendar |    ✅    |        |
| UUID v7 |     ✅ <br/>unix epoch      |    ✅    |   ✅    |

Implementation based on this [UUID RFC Draft](https://datatracker.ietf.org/doc/html/draft-ietf-uuidrev-rfc4122bis-03)

##  Quickstart

To use uuid4cats-effect in an existing SBT project with Scala 2.13 or a later version, add the following dependency to your
`build.sbt`:

```scala
libraryDependencies += "ant8e.tech" %% "uuid4cats-effect" % "<version>"
```

## Example

```scala
import cats.effect.IO
import tech.ant8e.uuid4cats.UUIDv6

for {
  generator<- UUIDv6.generator[IO]
  uuid1 <- generator.uuid
  uuid2 <-generator.uuid 
} yield (uuid1, uuid2)
```
                                
Uniqueness of generated time-based UUIDs is guaranteed when using the same generator. 
Collisions across generators are theoretically possible although unlikely.    
