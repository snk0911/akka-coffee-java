package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class CoffeeMain extends AbstractBehavior<CoffeeMain.StartMessage> {
    public static class StartMessage {}

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

        // 3 coffee machines with 10 units of coffee
        ActorRef<CoffeeMachine.Request> machine1 = getContext().spawn(CoffeeMachine.create(10), "Coffee Machine 1");
        ActorRef<CoffeeMachine.Request> machine2 = getContext().spawn(CoffeeMachine.create(10), "Coffee Machine 2");
        ActorRef<CoffeeMachine.Request> machine3 = getContext().spawn(CoffeeMachine.create(10), "Coffee Machine 3");

        // load balancer for coffee machines
        ActorRef<LoadBalancer.Mixed> loadBalance = getContext().spawn(LoadBalancer.create(new ActorRef[]{machine1, machine2, machine3}), "Load Balancer");

        // 4 customers to get money from
        ActorRef<Customer.Response> customer1 = getContext().spawn(Customer.create(cashRegister, loadBalance), "Customer Anna");
        ActorRef<Customer.Response> customer2 = getContext().spawn(Customer.create(cashRegister, loadBalance), "Customer Homer Simpson");
        ActorRef<Customer.Response> customer3 = getContext().spawn(Customer.create(cashRegister, loadBalance), "Customer Walter White");
        ActorRef<Customer.Response> customer4 = getContext().spawn(Customer.create(cashRegister, loadBalance), "Customer Harry");
        return this;
    }
}
