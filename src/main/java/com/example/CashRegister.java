package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class CashRegister extends AbstractBehavior<CashRegister.Request> {
    //balance of the current customer
    private int balance;

    public interface Request extends LoadBalancer.Mixed {
    }

    // is triggered when the customer wants to recharge the balance
    public static final class Recharge implements Request {
        public ActorRef<Customer.Response> sender;

        public Recharge(ActorRef<Customer.Response> sender) {
            this.sender = sender;
        }
    }

    // is triggered when load balancer asks for the current balance of the customer
    public static final class State implements Request {
        public final ActorRef<LoadBalancer.Mixed> sender;

        public State(ActorRef<LoadBalancer.Mixed> sender) {
            this.sender = sender;
        }
    }

    public static Behavior<Request> create(int balance) {
        return Behaviors.setup(context -> new CashRegister(context, balance));
    }

    private CashRegister(ActorContext<Request> context, int balance) {
        super(context);
        this.balance = balance;
    }

    @Override
    public Receive<Request> createReceive() {
        return newReceiveBuilder()
                .onMessage(Recharge.class, this::onRecharge)
                .onMessage(State.class, this::onState)
                .build();
    }

    // cash register recharges the balance of the customer
    private Behavior<Request> onRecharge(Recharge request) {
        getContext().getLog().info("Got a deposit request from {} (old balance: {})!", request.sender.path(), balance);
        this.balance += 1;
        // cash register sends a message with the new balance
        request.sender.tell(new Customer.CreditSuccess(balance));
        return this;
    }

    // cash register gives the balance status of the customer to load balancer
    private Behavior<Request> onState(State request) {
        getContext().getLog().info("Got a status request from {} ({})!", request.sender.path(), balance);
        if (this.balance > 0) {
            this.balance -= 1;
            request.sender.tell(new LoadBalancer.CreditSuccess(this.getContext().getSelf()));
        } else {
            request.sender.tell(new LoadBalancer.CreditFail(request.sender));
        }
        return this;
    }
}