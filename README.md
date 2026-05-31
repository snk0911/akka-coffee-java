# Akka Coffee (Java)

A small actor-based simulation of a coffee shop, built with **Akka Typed** (`akka-actor-typed`) in Java. Customers recharge their balance, request coffee, and a load balancer picks a machine to serve them. The project demonstrates message-driven concurrency, typed actors, and asynchronous coordination between independent components without shared mutable state.

This was the first project for the lecture *Electronic Business Processes* at TU Dortmund.

## What it does

Four customers continuously and independently do one of two things: top up their balance at the cash register, or ask for a coffee. There is no central loop driving them — each customer is an actor that reacts to incoming messages and decides its next action on its own. The full request flow for getting a coffee looks like this:

1. A **Customer** asks the **LoadBalancer** for a coffee.
2. The LoadBalancer asks the **CashRegister** whether the customer has enough balance.
3. If the balance is sufficient, the credit is deducted and the LoadBalancer queries all three **CoffeeMachines** for their remaining supply.
4. Once all machines have reported, the LoadBalancer selects the machine with the most coffee left and hands it to the customer.
5. The customer requests coffee directly from that machine, which serves it (or reports that it's empty).

If a customer has insufficient balance, they're told to recharge. If all machines run out, the system reports that it's out of coffee.

## Architecture

```
                ┌──────────────┐
   Customer ───►│ LoadBalancer │───► CashRegister   (balance check)
       ▲        └──────┬───────┘
       │               │
       │               ├──► CoffeeMachine 1
       │               ├──► CoffeeMachine 2   (supply query)
       │               └──► CoffeeMachine 3
       └──────────────────────────────────────┘
              (machine with most coffee serves the customer)
```

| Actor | Responsibility |
|-------|----------------|
| `CoffeeMain` | Root actor; spawns the cash register, three coffee machines (10 units each), the load balancer, and four customers. |
| `Customer` | Autonomous actor that randomly recharges or requests coffee and reacts to the responses. |
| `CashRegister` | Tracks each customer's balance, handles recharges, and confirms/denies sufficient credit. |
| `LoadBalancer` | Coordinates the coffee request: checks credit, polls all machines, and routes the customer to the machine with the most supply. |
| `CoffeeMachine` | Holds a coffee supply, reports its remaining amount, and serves coffee on request. |

Each actor communicates only through typed, immutable messages, which is the core idea the project illustrates: concurrent components that stay isolated and coordinate purely by message passing.

## Tech stack

- **Java**
- **Akka Typed** `akka-actor-typed` 2.6.19
- **Logback** for logging
- **Gradle** (primary) — a `build.sbt` is also included as an alternative

## Running it

With the Gradle wrapper:

```bash
./gradlew run
```

(`AkkaCoffeeStart` is the main class.) The actors then run on their own; press **ENTER** in the console to shut the system down. Activity is printed via the logger as customers recharge, request, and receive coffee.

## Project structure

```
src/main/java/com/example/
├── AkkaCoffeeStart.java   # entry point: boots the ActorSystem
├── CoffeeMain.java        # root actor, spawns all others
├── Customer.java          # customer actor
├── CashRegister.java      # balance management
├── LoadBalancer.java      # request coordination / machine selection
└── CoffeeMachine.java     # coffee supply
```

## Notes

This is a learning project focused on the actor model and concurrent message passing rather than on production concerns. Some design choices (fixed customer count, fixed-size balance store, in-console run) reflect that scope.

## Authors

Sewerin Kuss · Duc Anh Le · Janis Melon
