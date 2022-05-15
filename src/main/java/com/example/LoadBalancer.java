package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class LoadBalancer extends AbstractBehavior<LoadBalancer.Mixed> {

    private final ActorRef<CoffeeMachine.Request>[] coffeeMachinesList;
    private final ActorRef<CashRegister.Request> cashRegister;

    public interface Mixed {
    }

    // is triggered after cash register confirmed that the customer has enough money
    public static class CreditSuccess implements Mixed {
        public ActorRef<CashRegister.Request> sender;
        public ActorRef<Customer.Response> ofWhom;

        public CreditSuccess(ActorRef<CashRegister.Request> sender, ActorRef<Customer.Response> ofWhom) {
            this.sender = sender;
            this.ofWhom = ofWhom;
        }
    }

    // is triggered after cash register confirmed that the customer doesn't have enough money
    public static final class CreditFail implements Mixed {
        public ActorRef<CashRegister.Request> sender;
        public ActorRef<Customer.Response> ofWhom;

        public CreditFail(ActorRef<CashRegister.Request> sender, ActorRef<Customer.Response> ofWhom) {
            this.sender = sender;
            this.ofWhom = ofWhom;
        }
    }

    // is triggered after receiving the supply report from a coffee machine
    public static final class GetSupply implements Mixed {
        public ActorRef<CoffeeMachine.Request> sender;
        public ActorRef<Customer.Response> ofWhom;
        public ActorRef<CoffeeMachine.Request> coffeeMachineMax;
        public int max;

        public GetSupply(ActorRef<CoffeeMachine.Request> sender, ActorRef<Customer.Response> ofWhom, int remainingCoffee) {
            this.sender = sender;
            this.ofWhom = ofWhom;
            if (remainingCoffee > max) {
                max = remainingCoffee;
                coffeeMachineMax = sender;
            }
        }
    }

    // is triggered after customer asks load balancer for a coffee
    public static final class GetCoffee implements Mixed {
        public ActorRef<Customer.Response> sender;

        public GetCoffee(ActorRef<Customer.Response> sender) {
            this.sender = sender;
        }
    }

    public static Behavior<Mixed> create(ActorRef<CashRegister.Request> cashRegister, ActorRef<CoffeeMachine.Request>[] coffeeMachinesList) {
        return Behaviors.setup(context -> new LoadBalancer(context, cashRegister, coffeeMachinesList));
    }

    public LoadBalancer(ActorContext<Mixed> context, ActorRef<CashRegister.Request> cashRegister, ActorRef<CoffeeMachine.Request>[] coffeeMachinesList) {
        super(context);
        this.cashRegister = cashRegister;
        this.coffeeMachinesList = coffeeMachinesList;
    }

    @Override
    public Receive<Mixed> createReceive() {
        return newReceiveBuilder()
                .onMessage(CreditSuccess.class, this::onCreditSuccess)
                .onMessage(CreditFail.class, this::onCreditFail)
                .onMessage(GetCoffee.class, this::onGetCoffee)
                .onMessage(GetSupply.class, this::onGetSupply)
                .build();
    }

    // the customer has enough money for a coffee
    private Behavior<Mixed> onCreditSuccess(CreditSuccess respond) {
        //then the load balancer asks all the coffee machines for their supplies
        for (ActorRef<CoffeeMachine.Request> coffeeMachine : coffeeMachinesList) {
            coffeeMachine.tell(new CoffeeMachine.GiveSupply(this.getContext().getSelf(), respond.ofWhom, coffeeMachine));
        }
        return this;
    }

    // the customer doesn't have enough money for a coffee
    private Behavior<Mixed> onCreditFail(CreditFail response) {
        // load balancer forwards the error message to the customer
        response.ofWhom.tell(new Customer.BalanceFail());
        return this;
    }

    // customer asks load balancer for a coffee
    private Behavior<Mixed> onGetCoffee(GetCoffee request) {
        getContext().getLog().info("Got a get request from {}!", request.sender.path());
        // load balancer asks cash register if the customer has enough money for a coffee
        cashRegister.tell(new CashRegister.State(this.getContext().getSelf(), request.sender));
        return this;
    }

    // load balancer returns the coffee machine with the most coffee to the customer
    private Behavior<Mixed> onGetSupply(GetSupply response) {
        getContext().getLog().info("You can get a coffee from {}", response.coffeeMachineMax.path());
        response.ofWhom.tell(new Customer.GetCoffeeMachine(this.getContext().getSelf(), response.coffeeMachineMax));
        return this;
    }
}