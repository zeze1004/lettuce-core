## Kotlin API

Kotlin Coroutines are using Kotlin lightweight threads allowing to write
non-blocking code in an imperative way. On language side, suspending
functions provides an abstraction for asynchronous operations while on
library side kotlinx.coroutines provides functions like `async { }` and
types like `Flow`.

Lettuce ships with extensions to provide support for idiomatic Kotlin
use.

### Dependencies

Coroutines support is available when `kotlinx-coroutines-core` and
`kotlinx-coroutines-reactive` dependencies are on the classpath:

``` xml
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-coroutines-core</artifactId>
    <version>${kotlinx-coroutines.version}</version>
</dependency>
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-coroutines-reactive</artifactId>
    <version>${kotlinx-coroutines.version}</version>
</dependency>
```

### How does Reactive translate to Coroutines?

`Flow` is an equivalent to `Flux` in Coroutines world, suitable for hot
or cold streams, finite or infinite streams, with the following main
differences:

- `Flow` is push-based while `Flux` is a push-pull hybrid

- Backpressure is implemented via suspending functions

- `Flow` has only a single suspending collect method and operators are
  implemented as extensions

- Operators are easy to implement thanks to Coroutines

- Extensions allow to add custom operators to Flow

- Collect operations are suspending functions

- `map` operator supports asynchronous operations (no need for
  `flatMap`) since it takes a suspending function parameter

### Coroutines API based on reactive operations

Example for retrieving commands and using it:

``` kotlin
val api: RedisCoroutinesCommands<String, String> = connection.coroutines()

val foo1 = api.set("foo", "bar")
val foo2 = api.keys("fo*")
```

!!! NOTE
    Coroutine Extensions are experimental and require opt-in using
    `@ExperimentalLettuceCoroutinesApi`. The API ships with a reduced
    feature set. Deprecated methods and `StreamingChannel` are left out
    intentionally. Expect evolution towards a `Flow`-based API to consume
    large Redis responses.

### Extensions for existing APIs

#### Transactions DSL

Example for the synchronous API:

``` kotlin
val result: TransactionResult = connection.sync().multi {
    set("foo", "bar")
    get("foo")
}
```

Example for async with coroutines:

``` kotlin
val result: TransactionResult = connection.async().multi {
    set("foo", "bar")
    get("foo")
}
```