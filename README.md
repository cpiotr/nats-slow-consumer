# NATS Slow Consumer test case
## Description
Goal of this project is to illustrate the problem described in https://github.com/nats-io/nats-server/issues/2631 where 
a single slow consumer causes message delays in _other_ clients.

## Setup
### Messages
Each message consists only of 8-byte timestamp in milliseconds from epoch.
This project is limited in scope, therefore a message `delay` is considered significant if it exceeds `100ms`.

### Clients
* Request publishers are threads publishing messages to `Requests` subject.
* Confirmation publisher subscribes to `Requests` and publishes confirmations to `Confirmations` subject.
* Confirmation consumers are clients subscribing to `Confirmations` and checking if a message is delayed. If it is, an ERROR is logged.
* There is a single slow consumer which connects to NATS via TCP proxy with hardcoded delay of 900ms.

## Running
There are two dependencies to run the project:
* Maven
* JDK 17

Once installed, the following command can be used to run the tests:
```shell
mvn clean test
```

By default, 4 confirmation consumers are created, 1 slow consumer and 1 confirmation publisher. These values can be overridden from the commandline:
```shell
mvn clean test -Dnats.slow.proxy.port=23456 -Dnats.consumers.count=4 -Dnats.producers.count=4 -Dnats.consumers.slow.enabled=true
```
where:
* `nats.slow.proxy.port` denotes the port to be used by slow TCP proxy
* `nats.producers.count` represents the number of request producers
* `nats.consumers.count` represents the number of confirmation consumers
* `nats.consumers.slow.enabled` allows including/excluding the slow consumer in the test run

## Notes
Sometimes it takes _time_ to observe the delay. Below is the output from a sample execution:
```shell
15:33:59.123 [main] INFO 🐳 [nats:1.4.1] - Container nats:1.4.1 started in PT0.570026S
Oct 21, 2021 3:33:59 PM com.github.terma.javaniotcpserver.TcpServer start
INFO: Starting TcpServer on port 12233 with 4 workers
Oct 21, 2021 3:33:59 PM com.github.terma.javaniotcpserver.TcpServer start
INFO: TcpServer on port 12233 started
15:33:59.162 [pool-1-thread-1] INFO pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Consumer1 started
15:33:59.162 [pool-1-thread-2] INFO pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Consumer2 started
15:33:59.162 [pool-1-thread-3] INFO pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Consumer3 started
15:33:59.162 [pool-1-thread-4] INFO pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Consumer4 started
15:33:59.163 [pool-1-thread-5] INFO pl.ciruk.nats.slowconsumer.ConfirmationConsumer - SlowConsumer started
15:33:59.163 [pool-1-thread-6] INFO pl.ciruk.nats.slowconsumer.RequestProducer - Producer1 started
15:33:59.163 [pool-1-thread-7] INFO pl.ciruk.nats.slowconsumer.RequestProducer - Producer2 started
15:33:59.163 [pool-1-thread-8] INFO pl.ciruk.nats.slowconsumer.RequestProducer - Producer3 started
15:33:59.190 [pool-1-thread-1] INFO pl.ciruk.nats.slowconsumer.RequestProducer - Producer4 started
15:33:59.190 [pool-1-thread-2] INFO pl.ciruk.nats.slowconsumer.ConfirmationPublisher - ConfirmationPublisher started
15:34:17.025 [Consumer1:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 1008
15:34:23.577 [Consumer1:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 1008
15:34:23.577 [Consumer4:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 1008
15:34:23.577 [Consumer3:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 1008
15:34:23.577 [Consumer2:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 1008
15:37:09.995 [Consumer4:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 360
15:37:09.995 [Consumer2:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 360
15:37:09.995 [Consumer3:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 360
15:37:09.995 [Consumer4:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 360
15:37:09.995 [Consumer3:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 360
15:37:09.995 [Consumer4:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 360
15:37:09.995 [Consumer3:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 360
15:37:09.996 [Consumer4:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 361
15:37:09.995 [Consumer2:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 360
15:37:09.996 [Consumer2:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 361
15:37:09.995 [Consumer1:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 360
15:37:09.996 [Consumer2:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 361
15:37:09.996 [Consumer3:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 361
15:37:09.996 [Consumer1:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 361
15:37:09.996 [Consumer1:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 361
15:37:09.996 [Consumer1:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 361
15:37:14.983 [Consumer4:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 177
15:37:14.983 [Consumer3:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 177
15:37:14.983 [Consumer4:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 177
15:37:14.983 [Consumer3:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 177
15:37:14.983 [Consumer2:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 177
15:37:14.983 [Consumer1:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 177
15:37:14.983 [Consumer2:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 177
15:37:14.983 [Consumer1:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 177
15:37:14.983 [Consumer1:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 177
15:37:14.983 [Consumer4:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 177
15:37:14.983 [Consumer2:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 177
15:37:14.983 [Consumer1:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 177
15:37:14.983 [Consumer4:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 177
15:37:14.983 [Consumer2:3] ERROR pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Long delay detected: 177
```