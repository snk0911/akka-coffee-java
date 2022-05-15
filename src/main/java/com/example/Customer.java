package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

public class Customer extends AbstractBehavior<Customer.Response> {

    private final ActorRef<LoadBalancer.Mixed> loadBalancer;
    private final ActorRef<CashRegister.Request> cashRegister;

    public interface Response {
    }

    // is triggered after balance is successfully recharged
    public static final class RechargeSuccess implements Response {
        private final int balance;

        public RechargeSuccess(int balance) {
            this.balance = balance;
        }
    }

    // is triggered when load balancer sends a message
    // that the current balance is not enough for a coffee
    public static final class BalanceFail implements Response {
    }

    // is triggered after load balancer returns the machine with the most remaining coffee
    public static final class GetCoffeeMachine implements Response {
        public ActorRef<LoadBalancer.Mixed> sender;
        public ActorRef<CoffeeMachine.Request> coffeeMachine;

        public GetCoffeeMachine(ActorRef<LoadBalancer.Mixed> sender, ActorRef<CoffeeMachine.Request> coffeeMachine) {
            this.sender = sender;
            this.coffeeMachine = coffeeMachine;
        }
    }

    // is triggered after the customer has received a coffee from the machine
    public static final class GetSuccess implements Response {
    }

    // is triggered when the chosen coffee machine is empty
    public static final class GetFail implements Response {
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
                .onMessage(RechargeSuccess.class, this::onRechargeSuccess)
                .onMessage(BalanceFail.class, this::onBalanceFail)
                .onMessage(GetCoffeeMachine.class, this::onGetCoffeeMachine)
                //TODO: fix GetSuccess, GetFail und on-Aktionen.
                //.onMessage(GetSuccess.class, this::onGetSuccess)
                //.onMessage(GetFail.class, this::onGetFail)
                .build();
    }

    // the cash register confirms that the recharge was successful and shows the new balance
    private Behavior<Response> onRechargeSuccess(RechargeSuccess command) {
        getContext().getLog().info("You have successfully recharged your balance. Current balance: {}", command.balance);
        if (Math.random() < 0.5) {
            cashRegister.tell(new CashRegister.Recharge(this.getContext().getSelf()));
        } else {
            loadBalancer.tell(new LoadBalancer.GetCoffee(this.getContext().getSelf()));
        }
        return this;
    }

    // the customer doesn't have enough money for a coffee
    private Behavior<Response> onBalanceFail(BalanceFail command) {
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
        getContext().getLog().info("You can now take coffee from {}", command.coffeeMachine.path());
        command.coffeeMachine.tell(new CoffeeMachine.GetCoffee(this.getContext().getSelf()));
        return this;
    }

    // customer successfully received a coffee from the coffee machine
    private Behavior<Response> onGetSuccess() {
        getContext().getLog().info("Here is your coffee!");
        if (Math.random() < 0.5) {
            cashRegister.tell(new CashRegister.Recharge(this.getContext().getSelf()));
        } else {
            loadBalancer.tell(new LoadBalancer.GetCoffee(this.getContext().getSelf()));
        }
        return this;
    }

    // all the coffee machines have run out of coffee
    private Behavior<Response> onGetFail() {
        getContext().getLog().info("Sorry, we have run out of coffee. Please try again later.");
        return Behaviors.stopped();
    }
}