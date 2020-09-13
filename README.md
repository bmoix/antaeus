## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Antaeus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!

# Solution

## Assumptions

- Invoices should be paid once.
- Invoices are paid at the beginning of the desired period (eg. monthly).
- Invoices are not recurring. Paying an invoice doesn't create a new invoice in the next period.
- Invoices that can't be paid are marked as `ERROR`. They are not re-scheduled for a retry.
- Invoices that fail for external factors should be re-scheduled.
- Invoices with a wrong currency should be updated with the customer's currency and re-scheduled.

## Design

The solution is built around 2 main services: `BillingService` and `SchedulingService`.
They communicate with each other using channels.

It follows the *producer-consumer* pattern. The `SchedulingService` produces the data
that the `BillingService` consumes.

### Billing

The `BillingService` processes invoices that are ready to be paid.

It works by launching a worker (by using coroutines) that listens to the `processingChannel`.
The `processingChannel` is the main source of input data.
Right now, the data comes from the `SchedulingService`, but it could come from anyone that
sends data through the `processingChannel`.

With this implementation, it would be really easy to add more workers to listen to the
`processingChannel` and consume its data.

Invoices are processed only **ONCE**. Invoices have different states that indicate where in
which stage they are. Using a combination of the states and DB locks, we can avoid double spends.

When a payment fails for external factors, it is sent to the `retryChannel`.
The `ScheduleService` is the one that listens to the `retryChannel` and decides
when to schedule its retry.
If we want to do something else with the failed invoices, we can change that behavior very easily
by changing the service that listens the `retryChannel`.

Some failures are *unrecoverable*. Those are marked as `ERROR` and they are not retried.

### Scheduling

The `SchedulingService` decides when the invoices will be processed.

It works with 3 workers:
1. Initial scheduling of failed invoices by external factors.
2. Main worker that schedules payments with a given periodicity.
3. Worker that schedules retries with an exponential backoff.

The main scheduling is done by computing the remaining time until the start of the next
period, and sleep until then.
Once the worker wakes up, checks the DB for `PENDING` invoices and sends them to the `BillingService`
through the `processingChannel`.

The retry logic is done by listening to the data sent through the `retryChannel`.
The data contains the invoice's ID and the number of times that it has been processed (and failed).
Using the information of the number of attempts to process the invoice, we can easily implement
an exponential backoff.

### Exchanges

Exchanges are handled by the `ExchangeService`.

The `ExchangeService` uses an external `CurrencyExchangedProvider`.

It's an easy way to exchange values of different currencies.

It can also "fix" the currency of an invoice to match the customer's currency.

## Additional features

- Different periodicity values: hourly, daily, weekly and monthly.
- Implementation of the external providers. 

## Testing

Unit testing provided for core components with enough coverage of the main use cases and some edge cases.

Integration tests not present.

Manually tested the whole project.

## Improvements

- Handle DB failures gracefully.
- Error handling of the currency exchange.
- Better testing: integration tests, scheduling service, etc.
- Better logs.
- Better coroutine management.
- Launch more coroutines to process bills concurrently.
- Stop retrying after a certain amount of retries and mark the invoice as `ERROR`.
- Create a new table to keep track of the billing history/attempts of an invoice.
- Add support for recurring invoices.
- Add some sort of notification system to alert customers when they are out of funds.
- Better exchange computations (truncate, round, etc)
- Cache the exchange rates and update the cache periodically.
- Idiomatic Kotlin.

## Time spent

Since I'm new to Kotlin, I initially spent some time reading 
[the official documentation](https://kotlinlang.org/docs/tutorials/getting-started.html)
and doing some of the [Koans](https://kotlinlang.org/docs/tutorials/koans.html).

Working on the project, I spent around 12h divided in 3 afternoons.

Although I felt a bit clumsy with Kotlin and Gradle, I really loved it.
It looks like a really powerful and expressive language. 

## Example output

*Note:* Since the workers work concurrently, you may see outputs from different bills "merged". 

Starting the scheduling service with an hourly periodicity
```
[main] INFO io.pleo.antaeus.core.services.SchedulingService - Starting the Scheduling Service...
[DefaultDispatcher-worker-2] INFO io.pleo.antaeus.core.services.SchedulingService - Starting initializer worker...
[DefaultDispatcher-worker-3] INFO io.pleo.antaeus.core.services.SchedulingService - Staring scheduler worker with a HOURLY periodicity...
[DefaultDispatcher-worker-2] INFO io.pleo.antaeus.core.services.SchedulingService - Scheduling 0 failed invoices
[DefaultDispatcher-worker-4] INFO io.pleo.antaeus.core.services.SchedulingService - Starting retrier worker...
[DefaultDispatcher-worker-3] INFO io.pleo.antaeus.core.services.SchedulingService - Going to sleep for 79636 ms until 2020-09-13T17:59:59.999671Z[UTC]...
```

Processing an invoice that fails due to insufficient funds and is re-scheduled
```
[DefaultDispatcher-worker-7] WARN io.pleo.antaeus.core.services.BillingService - Invoice '1341' from customer '69' FAILED. Retrying...                                   
[DefaultDispatcher-worker-7] INFO io.pleo.antaeus.core.services.SchedulingService - Scheduling retry for invoice '1341' in 2000 ms
```

Processing an invoice that fails for a network error and schedule its retry
```
[DefaultDispatcher-worker-4] INFO io.pleo.antaeus.core.services.BillingService - Processing invoice '396'
[DefaultDispatcher-worker-4] WARN io.pleo.antaeus.core.services.BillingService - Failed to pay invoice '396 from customer '40'. A network error happened please try again.
[DefaultDispatcher-worker-2] INFO io.pleo.antaeus.core.services.SchedulingService - Scheduling retry for invoice '396' in 1000 ms
```

Processing an invoice that has incorrect currency and doing an exchange
```
[DefaultDispatcher-worker-4] WARN io.pleo.antaeus.core.services.BillingService - Failed to pay invoice '88 from customer '18'. Currency of invoice '88' does not match currency of customer '18'
[DefaultDispatcher-worker-4] INFO io.pleo.antaeus.core.services.ExchangeService - Fixing currency of invoice '88'
[DefaultDispatcher-worker-4] INFO io.pleo.antaeus.core.services.ExchangeService - Exchange: 93.35 EUR -> 694.5744090 DKK
[DefaultDispatcher-worker-2] INFO io.pleo.antaeus.core.services.SchedulingService - Scheduling retry for invoice '88' in 1000 ms
```