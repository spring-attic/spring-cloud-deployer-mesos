# Spring Cloud Deployer Mesos
![Maven Central](https://img.shields.io/maven-central/v/com.trustedchoice/spring-cloud-deployer-mesos.svg)
[![Build Status](https://travis-ci.org/trustedchoice/spring-cloud-deployer-mesos.svg?branch=master)](https://travis-ci.org/trustedchoice/spring-cloud-deployer-mesos)
[![codecov](https://codecov.io/gh/trustedchoice/spring-cloud-deployer-mesos/branch/master/graph/badge.svg)](https://codecov.io/gh/trustedchoice/spring-cloud-deployer-mesos)
[![Gitter](https://img.shields.io/gitter/room/spring-cloud/spring-cloud-dataflow.svg)](https://gitter.im/spring-cloud/spring-cloud-dataflow)

A [Spring Cloud Deployer](https://github.com/spring-cloud/spring-cloud-deployer) implementation for deploying long-lived
streaming applications to [Marathon](https://mesosphere.github.io/marathon/) and short-lived tasks to 
[Chronos](https://mesos.github.io/chronos/) on Apache Mesos.

## Chronos Compatibility

Version 3 of Chronos is required.  Testing has been done against v3.0.1.

## Building

Build the project without running tests using:

```
./mvnw clean install
```
