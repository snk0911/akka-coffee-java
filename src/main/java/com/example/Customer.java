package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

public class Customer extends AbstractBehavior<Customer.Response> {

    private final ActorRef<LoadBalancer.Mixed> loadBalancer;
    private final ActorRef<CashRegister.Request> cashRegister;
    private ActorRef<CoffeeMachine.Request> coffeeMachine;

    public interface Response extends LoadBalancer.Mixed {

    }

    // is triggered after balance is successfully recharged
    public static final class CreditSuccess implements Response {
        private int balance;

        public CreditSuccess(int balance) {
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

    public static final class GetSuccess implements Response{
    }

    public static final class GetFail implements Response {
    }

    public static final class CreditFail implements Response {
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
                .onMessage(CreditSuccess.class, this::onCreditSuccess)
                .onMessage(CreditFail.class, this::onCreditFail)
                .onMessage(GetCoffeeMachine.class, this::onGetCoffeeMachine)
                .onMessage(GetSuccess.class, this::onGetSuccess)
                .onMessage(GetFail.class, this::onGetFail)
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

    // the customer doesn't have enough money for a coffee
    private Behavior<Response> onCreditFail(CreditFail command) {
        getContext().getLog().info("Your current balance is insufficient for a coffee. Please try again.");
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

    // customer successfully received a coffee from the coffee machine
    private Behavior<Response> onGetSuccess(GetCoffeeMachine command) {
        getContext().getLog().info("Here is your coffee!");
        if (Math.random() < 0.5) {
            cashRegister.tell(new CashRegister.Recharge(this.getContext().getSelf()));
        } else {
            loadBalancer.tell(new LoadBalancer.GetCoffee(this.getContext().getSelf()));
        }
        return this;
    }

    // all the coffee machines have run out of coffee
    private Behavior<Response> onGetFail(GetCoffeeMachine command) {
        getContext().getLog().info("Sorry, we have run out of coffee. Please try again later.");
        return this;
    }
}