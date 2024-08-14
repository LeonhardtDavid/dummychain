# dummychain

![badge](https://github.com/LeonhardtDavid/dummychain/actions/workflows/scala.yml/badge.svg)

## Requirements

To be able to run the service locally and test it, you need the following tools installed:

- Java 11 (It should work with newer versions, but it hasn't been tested)
- [sbt](https://www.scala-sbt.org/download/)

## Run the service

To run the service locally use the following command:

```shell
sbt service/run
```

If you want to work on some changes, and enable hot reload to restart the service on changes, run:

```shell
sbt "~service/reStart"
```

The service has a couple of configurations that can be
seen [here](/modules/service/src/main/resources/application.conf).
If you want to try different settings, you can modify that file or use and environment variable.

|                Config                 |         Env. Var.          | Default | Description                                                                                                 |
|:-------------------------------------:|:--------------------------:|:-------:|-------------------------------------------------------------------------------------------------------------|
|              server.port              |            PORT            |  8080   | Service listening port                                                                                      |
|      blockchain.number-of-zeros       |      NUMBER_OF_ZEROS       |    2    | Number of zeros expected at the beginning of the block hash                                                 |
| blockchain.max-transactions-per-block | MAX_TRANSACTIONS_PER_BLOCK |    5    | Number of transactions per block. Once the latests block has this many transactions, a new block is created |

## Running the tests

To run the test, just use the following command:

```shell
sbt test
```

Also, you can check the GitHub Actions [here](https://github.com/LeonhardtDavid/dummychain/actions).

## Design and Decisions

// TODO

## Production Readiness

// TODO

- Healthcheck
- Concurrency / Race Conditions
- Docker / Kubernetes / Helm