package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class Customer extends AbstractBehavior<Customer.Response> {

    private final ActorRef<LoadBalancer.Mixed> loadBalancer;
    private final ActorRef<CashRegister.Request> cashRegister;
    private ActorRef<CoffeeMachine.Request> coffeeMachine;

    public interface Response extends LoadBalancer.Mixed {
    }

    public static final class CreditSuccess implements Response {
        private int balance;
        public final ActorRef<CashRegister.Request> sender;

        public CreditSuccess(ActorRef<CashRegister.Request> sender, int balance) {
            this.sender = sender;
            this.balance = balance;
        }
    }

    public static final class GetCoffeeMachine implements Response {
        public ActorRef<LoadBalancer.Mixed> sender;
        public ActorRef<CoffeeMachine.Request> coffeeMachine;

        public GetCoffeeMachine(ActorRef<LoadBalancer.Mixed> sender, ActorRef<CoffeeMachine.Request> coffeeMachine) {
            this.sender = sender;
            this.coffeeMachine = coffeeMachine;
        }
    }

    public static final class GetSuccess implements Response {
    }

    public static final class Fail implements Response {
    }

    public static Behavior<Response> create(ActorRef<CashRegister.Request> cashRegister, ActorRef<LoadBalancer.Mixed> loadBalancer) {
        return Behaviors.setup(context -> new Customer(context, loadBalancer, cashRegister));
    }

    private Customer(ActorContext<Response> context, ActorRef<LoadBalancer.Mixed> loadBalancer, ActorRef<CashRegister.Request> cashRegister) {
        super(context);
        this.loadBalancer = loadBalancer;
        this.cashRegister = cashRegister;
    }

    @Override
    public Receive<Response> createReceive() {
        return newReceiveBuilder()
                // credit success
                .onMessage(CreditSuccess.class, this::onCreditSuccess)
                .onMessage(Fail.class, this::onFail)
                .onMessage(GetCoffeeMachine.class, this::onGetCoffeeMachine)
                .build();
    }

    // the cash register confirms that the recharge was successful and shows the new balance
    private Behavior<Response> onCreditSuccess(CreditSuccess command) {
        getContext().getLog().info("You have successfully recharged your balance. Current balance: {}", command.balance);
        if (Math.random() < 0.5) {
            cashRegister.tell(new CashRegister.Recharge(this.getContext().getSelf()));
        } else {
            loadBalancer.tell(new LoadBalancer.GetCoffee(this.getContext().getSelf()));
        }
        return this;
    }

    // not yet
    private Behavior<Response> onFail(Fail command) {
        getContext().getLog().info("Fail");
        if (Math.random() < 0.5) {
            cashRegister.tell(new CashRegister.Recharge(this.getContext().getSelf()));
        } else {
            loadBalancer.tell(new LoadBalancer.GetCoffee(this.getContext().getSelf()));
        }
        return this;
    }

    // customer receives the coffee machine with the most remaining supply
    private Behavior<Response> onGetCoffeeMachine(GetCoffeeMachine command) {
        getContext().getLog().info("You can know take coffee from {}", command.coffeeMachine);
        command.coffeeMachine.tell(new CoffeeMachine.GetCoffee(this.getContext().getSelf()));
        return this;
    }
}
