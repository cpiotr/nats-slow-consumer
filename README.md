# NATS Slow Consumer test case
## Description
Goal of this project is to illustrate the problem described in https://github.com/nats-io/nats-server/issues/2631 where 
a single slow consumer causes message delays in _other_ clients.

## Setup
### Messages
Each message weighs 1024 bytes. It consists of 8-byte timestamp in milliseconds from epoch which is followed by insignificant bytes. 
The problem was not reproducible with smaller messages.
Message `delay` is considered significant if it exceeds `1000ms`.

### Clients
* Request publishers are threads publishing messages to `Requests` subject.
* Confirmation publisher subscribes to `Requests` and publishes confirmations to `Confirmations` subject.
* Confirmation consumers are clients subscribing to `Confirmations` and checking if a message is delayed. If it is, an ERROR is logged.
* There is a single slow consumer which connects to NATS via TCP proxy with hardcoded delay of 200ms.

## Slow proxy
There is a TCP proxy configured to inject a hardcoded delay of 200ms for incoming packets. It is based on [java-nio-tcp-proxy](https://github.com/terma/java-nio-tcp-proxy/). 
Some classes had to be copied from Github to allow injecting custom code.

## Running
There are two dependencies to run the project:
* JDK 17
* Docker daemon

Once installed, the following command can be used to run the tests:
```shell
./mvnw clean test
```

By default, 4 confirmation consumers are created, 1 slow consumer and 1 confirmation publisher. These values can be overridden from the commandline:
```shell
./mvnw clean test -Dnats.slow.proxy.port=23456 -Dnats.consumers.count=4 -Dnats.producers.count=4 -Dnats.consumers.slow.count=1
```
where:
* `nats.slow.proxy.port` denotes the port to be used by the slow TCP proxy
* `nats.producers.count` represents the number of request producers
* `nats.consumers.count` represents the number of confirmation consumers
* `nats.consumers.slow.count` represents the number of **slow** consumers of confirmations

## Results
### Baseline
```shell
11:43:23.525 [main] INFO 🐳 [nats:1.4.1] - Container nats:1.4.1 started in PT0.540625S
Oct 25, 2021 11:43:23 AM com.github.terma.javaniotcpserver.TcpServer start
INFO: Starting TcpServer on port 12233 with 4 workers
Oct 25, 2021 11:43:23 AM com.github.terma.javaniotcpserver.TcpServer start
INFO: TcpServer on port 12233 started
11:43:23.566 [pool-1-thread-1] INFO pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Consumer1 started
11:43:23.566 [pool-1-thread-2] INFO pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Consumer2 started
11:43:23.566 [pool-1-thread-3] INFO pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Consumer3 started
11:43:23.567 [pool-1-thread-4] INFO pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Consumer4 started
11:43:23.567 [pool-1-thread-5] INFO pl.ciruk.nats.slowconsumer.RequestProducer - Producer1 started
11:43:23.567 [pool-1-thread-6] INFO pl.ciruk.nats.slowconsumer.RequestProducer - Producer2 started
11:43:23.567 [pool-1-thread-7] INFO pl.ciruk.nats.slowconsumer.RequestProducer - Producer3 started
11:43:23.568 [pool-1-thread-8] INFO pl.ciruk.nats.slowconsumer.RequestProducer - Producer4 started
11:43:23.593 [pool-1-thread-1] INFO pl.ciruk.nats.slowconsumer.ConfirmationPublisher - ConfirmationPublisher started
11:43:28.561 [pool-2-thread-1] DEBUG pl.ciruk.nats.slowconsumer.SlowConsumerTest - Delays: count=1459, p95=1ms, p99=1ms, p100=4ms
...
11:43:48.589 [pool-2-thread-1] DEBUG pl.ciruk.nats.slowconsumer.SlowConsumerTest - Delays: count=7130, p95=1ms, p99=1ms, p100=16ms
...
11:52:24.307 [pool-2-thread-1] DEBUG pl.ciruk.nats.slowconsumer.SlowConsumerTest - Delays: count=154143, p95=1ms, p99=1ms, p100=199ms
...
12:02:25.135 [pool-2-thread-1] DEBUG pl.ciruk.nats.slowconsumer.SlowConsumerTest - Delays: count=325783, p95=1ms, p99=1ms, p100=199ms
```

### Problem occurrence
Please, note that sometimes it takes 50k messages to observe the delay.
```shell
12:24:04.020 [main] INFO 🐳 [nats:1.4.1] - Container nats:1.4.1 started in PT1.187668S
Oct 25, 2021 12:24:04 PM com.github.terma.javaniotcpserver.TcpServer start
INFO: Starting TcpServer on port 12233 with 4 workers
Oct 25, 2021 12:24:04 PM com.github.terma.javaniotcpserver.TcpServer start
INFO: TcpServer on port 12233 started
12:24:04.121 [pool-1-thread-1] INFO pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Consumer1 started
12:24:04.121 [pool-1-thread-2] INFO pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Consumer2 started
12:24:04.122 [pool-1-thread-3] INFO pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Consumer3 started
12:24:04.122 [pool-1-thread-4] INFO pl.ciruk.nats.slowconsumer.ConfirmationConsumer - Consumer4 started
12:24:04.123 [pool-1-thread-5] INFO pl.ciruk.nats.slowconsumer.ConfirmationConsumer - SlowConsumer started
12:24:04.124 [pool-1-thread-6] INFO pl.ciruk.nats.slowconsumer.RequestProducer - Producer1 started
12:24:04.124 [pool-1-thread-7] INFO pl.ciruk.nats.slowconsumer.RequestProducer - Producer2 started
12:24:04.125 [pool-1-thread-8] INFO pl.ciruk.nats.slowconsumer.RequestProducer - Producer3 started
12:24:04.202 [pool-1-thread-1] INFO pl.ciruk.nats.slowconsumer.RequestProducer - Producer4 started
12:24:04.202 [pool-1-thread-2] INFO pl.ciruk.nats.slowconsumer.ConfirmationPublisher - ConfirmationPublisher started
12:24:09.111 [pool-2-thread-1] DEBUG pl.ciruk.nats.slowconsumer.SlowConsumerTest - Delays: count=1370, p95=1ms, p99=1ms, p100=8ms
...
12:24:24.125 [pool-2-thread-1] DEBUG pl.ciruk.nats.slowconsumer.SlowConsumerTest - Delays: count=5678, p95=1ms, p99=1ms, p100=13ms
...
12:24:39.139 [pool-2-thread-1] DEBUG pl.ciruk.nats.slowconsumer.SlowConsumerTest - Delays: count=10113, p95=1ms, p99=1ms, p100=37ms
...
12:24:54.145 [pool-2-thread-1] DEBUG pl.ciruk.nats.slowconsumer.SlowConsumerTest - Delays: count=14491, p95=1ms, p99=1ms, p100=1008ms
...
12:25:59.190 [pool-2-thread-1] DEBUG pl.ciruk.nats.slowconsumer.SlowConsumerTest - Delays: count=33939, p95=1ms, p99=1ms, p100=1008ms
```
