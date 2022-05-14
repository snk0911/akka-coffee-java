package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

public class CoffeeMain extends AbstractBehavior<CoffeeMain.StartMessage> {
    public static class StartMessage {}

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

    private Behavior<StartMessage> onStartMessage(StartMessage command) {

        // cash register which determines if enough balance is given
        cashRegister = getContext().spawn(CashRegister.create(0), "Cash Register");

        // 3 coffee machines with 10 units of coffee
        machine1 = getContext().spawn(CoffeeMachine.create(10), "Coffee Machine 1");
        machine2 = getContext().spawn(CoffeeMachine.create(10), "Coffee Machine 2");
        machine3 = getContext().spawn(CoffeeMachine.create(10), "Coffee Machine 3");

        // load balancer for coffee machines
        loadBalancer = getContext().spawn(LoadBalancer.create(new ActorRef[]{machine1, machine2, machine3}), "Load Balancer");

        // 4 customers to get money from
        customer1 = getContext().spawn(Customer.create(cashRegister, loadBalancer), "Customer Anna");
        customer2 = getContext().spawn(Customer.create(cashRegister, loadBalancer), "Customer Homer Simpson");
        customer3 = getContext().spawn(Customer.create(cashRegister, loadBalancer), "Customer Walter White");
        customer4 = getContext().spawn(Customer.create(cashRegister, loadBalancer), "Customer Harry");
        return this;
    }
}
