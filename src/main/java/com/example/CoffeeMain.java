package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

public class CoffeeMain extends AbstractBehavior<CoffeeMain.StartMessage> {
    public static class StartMessage {}

    ActorRef<SomeActor.SomeMessage> someActor;

    public static Behavior<StartMessage> create() {
        return Behaviors.setup(CoffeeMain::new);
    }

    private CoffeeMain(ActorContext<StartMessage> context) {
        super(context);
    }

    @Override
    public Receive<StartMessage> createReceive() {
        return newReceiveBuilder().onMessage(StartMessage.class, this::onStartMessage).build();
    }

    private Behavior<StartMessage> onStartMessage(StartMessage command) {

        // cash register which determines if enough balance is given
        ActorRef<CashRegister.Request> cashRegister = getContext().spawn(CashRegister.create(0), "Cash Register");

        // load balancer for coffee machines
        ActorRef<LoadBalancer.Mixed> loadBalance = getContext().spawn(CashRegister.create(10), "Load Balancer");

        // 3 coffee machines with 10 units of coffee
        ActorRef<CoffeeMachine.Request> machine1 = getContext().spawn(CoffeeMachine.create(10), "Coffee Machine 1");
        ActorRef<CoffeeMachine.Request> machine2 = getContext().spawn(CoffeeMachine.create(10), "Coffee Machine 2");
        ActorRef<CoffeeMachine.Request> machine3 = getContext().spawn(CoffeeMachine.create(10), "Coffee Machine 3");

        // 4 customers to get money from
        ActorRef<Customer.Response> customer1 = getContext().spawn(Customer.create(lagerverwaltung), "Customer Anna");
        ActorRef<Customer.Response> customer2 = getContext().spawn(Customer.create(lagerverwaltung), "Customer Homer Simpson");
        ActorRef<Customer.Response> customer3 = getContext().spawn(Customer.create(lagerverwaltung), "Customer Walter White");
        ActorRef<Customer.Response> customer4 = getContext().spawn(Customer.create(lagerverwaltung), "Customer Harry");
        return this;
    }
}
