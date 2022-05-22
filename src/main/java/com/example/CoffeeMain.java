package com.example;
// Sewerin Kuss 201346
// Duc Anh Le 230662
// Janis Melon 209928

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class CoffeeMain extends AbstractBehavior<CoffeeMain.StartMessage> {
    public static class StartMessage {
    }

    ActorRef<CashRegister.Request> cashRegister;

    ActorRef<CoffeeMachine.Request> machine1;
    ActorRef<CoffeeMachine.Request> machine2;
    ActorRef<CoffeeMachine.Request> machine3;

    ActorRef<LoadBalancer.Mixed> loadBalancer;

    ActorRef<Customer.Response> customer1;
    ActorRef<Customer.Response> customer2;
    ActorRef<Customer.Response> customer3;
    ActorRef<Customer.Response> customer4;

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

    private Behavior<StartMessage> onStartMessage(StartMessage command) throws InterruptedException {

        // cash register which determines if enough balance is given
        cashRegister = getContext().spawn(CashRegister.create(), "CashRegister");

        // 3 coffee machines with 10 units of coffee
        machine1 = getContext().spawn(CoffeeMachine.create(10), "CoffeeMachine1");
        machine2 = getContext().spawn(CoffeeMachine.create(10), "CoffeeMachine2");
        machine3 = getContext().spawn(CoffeeMachine.create(10), "CoffeeMachine3");

        // load balancer for coffee machines
        loadBalancer = getContext().spawn(LoadBalancer.create(cashRegister, new ActorRef[]{machine1, machine2, machine3}), "LoadBalancer");

        // 4 customers to get money from
        customer1 = getContext().spawn(Customer.create(cashRegister, loadBalancer), "Anna");
        Thread.sleep(500);
        customer2 = getContext().spawn(Customer.create(cashRegister, loadBalancer), "HomerSimpson");
        Thread.sleep(500);
        customer3 = getContext().spawn(Customer.create(cashRegister, loadBalancer), "WalterWhite");
        Thread.sleep(500);
        customer4 = getContext().spawn(Customer.create(cashRegister, loadBalancer), "Harry");

        return this;
    }
}
