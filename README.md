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

### Configurations

The service has a couple of configurations that can be
seen [here](/modules/service/src/main/resources/application.conf).
If you want to try different settings, you can modify that file or use and environment variable.

|                Config                 |         Env. Var.          | Default | Description                                                                                                 |
|:-------------------------------------:|:--------------------------:|:-------:|-------------------------------------------------------------------------------------------------------------|
|              server.port              |            PORT            |  8080   | Service listening port                                                                                      |
|      blockchain.number-of-zeros       |      NUMBER_OF_ZEROS       |    2    | Number of zeros expected at the beginning of the block hash                                                 |
| blockchain.max-transactions-per-block | MAX_TRANSACTIONS_PER_BLOCK |    5    | Number of transactions per block. Once the latests block has this many transactions, a new block is created |

### API

Once the application is running, you can check for the API documentation on http://localhost:8080/docs.

For simplicity for this implementation, the addresses are just the public key, no hashed.  
When the service starts, a genesis block is generated for a particular address, you can see a similar log as follows to get the private and public keys for that address.
![Log Keys](/docs/keys-log.png)

To generate more keys, for example to have a destination address, you can generate a new key pair with `sbt "helpers/run generate"`, and use the public key generated as the address.

Finally, you will need to sign the request, for that you can use the following command:
```shell
sbt "helpers/run sign <source_address> <destination_address> <amount> <private_key>"
```

For example:
```shell
sbt "helpers/run sign MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEmKbo9emnhQNXpR5Xoqm_0bN9bxUwAU_dRyk6nvWwJbut41WmKN6IvpAFGfXrGiDkD0Fxd4Z6D6a7xInKlyQRrA MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEAhTGwJlILrlxwWhg1LmCjSu4T098LB4xQbiVkZDvWcZPuDvf6LFW7h8JoKy94Ho-wTXXl7dQ6cC-Ot2TjClkhg 90 MIGNAgEAMBAGByqGSM49AgEGBSuBBAAKBHYwdAIBAQQgKHg4hP3DF2ZabuzSEQ1CtlJik4gB2uuSMBJ91xKXhAWgBwYFK4EEAAqhRANCAASYpuj16aeFA1elHleiqb_Rs31vFTABT91HKTqe9bAlu63jVaYo3oi-kAUZ9esaIOQPQXF3hnoPprvEicqXJBGs"
```

Request example using `curl`:
```shell
curl --location 'http://localhost:8080/api/transactions' \
--header 'Content-Type: application/json' \
--data '{
    "source": "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEmKbo9emnhQNXpR5Xoqm_0bN9bxUwAU_dRyk6nvWwJbut41WmKN6IvpAFGfXrGiDkD0Fxd4Z6D6a7xInKlyQRrA",
    "destination": "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEAhTGwJlILrlxwWhg1LmCjSu4T098LB4xQbiVkZDvWcZPuDvf6LFW7h8JoKy94Ho-wTXXl7dQ6cC-Ot2TjClkhg",
    "amount": 90,
    "signature": "MEYCIQDPoGmSHU4aMcxVlH1AcerCY44YS0z3MrKB1BjLePTvEAIhAJ2zOJrq8Ez7PwFxX19MVOfrC0HoGDNc3e7y-f7XYBpR"
}'
```

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