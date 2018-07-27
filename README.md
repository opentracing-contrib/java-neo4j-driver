[![Build Status][ci-img]][ci] [![Coverage Status][cov-img]][cov] [![Released Version][maven-img]][maven]

# OpenTracing Neo4j Driver Instrumentation
OpenTracing instrumentation for Neo4j Driver.

## Installation

pom.xml
```xml
<dependency>
    <groupId>io.opentracing.contrib</groupId>
    <artifactId>opentracing-neo4j-driver</artifactId>
    <version>VERSION</version>
</dependency>
```

## Usage

```java
// Instantiate tracer
Tracer tracer = ...

// Decorate Neo4j Driver with Tracing Driver
Driver driver = new TracingDriver(GraphDatabase.driver(...), tracer);

```

### OGM
```java
// Create BoltDriver from decorated neo4j driver:
Driver boltDriver = new BoltDriver(new TracingDriver(GraphDatabase.driver(...), tracer));

// Create Session Factory
SessionFactory sessionFactory = new SessionFactory(boltDriver);
```

## License

[Apache 2.0 License](./LICENSE).

[ci-img]: https://travis-ci.org/opentracing-contrib/java-neo4j-driver.svg?branch=master
[ci]: https://travis-ci.org/opentracing-contrib/java-neo4j-driver
[cov-img]: https://coveralls.io/repos/github/opentracing-contrib/java-neo4j-driver/badge.svg?branch=master
[cov]: https://coveralls.io/github/opentracing-contrib/java-neo4j-driver?branch=master
[maven-img]: https://img.shields.io/maven-central/v/io.opentracing.contrib/opentracing-neo4j-driver.svg
[maven]: http://search.maven.org/#search%7Cga%7C1%7Copentracing-neo4j-driver

