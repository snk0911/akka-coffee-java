package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.japi.Pair;

import java.util.Arrays;

public class CashRegister extends AbstractBehavior<CashRegister.Request> {

    //balance database
    private final Pair<ActorRef<Customer.Response>, Integer>[] database = new Pair[4];
    // private final Pair<ActorRef<Customer.Response>, Integer>[] database = new Pair[4];
    private int newSlot = 0;

    public interface Request {
    }

    /**
     * Is triggered when the customer wants to recharge the balance.
     */
    public static final class Recharge implements Request {
        public ActorRef<Customer.Response> sender;

        public Recharge(ActorRef<Customer.Response> sender) {
            this.sender = sender;
        }
    }

    /**
     * Is triggered when load balancer asks for the current balance of the customer.
     */
    public static final class State implements Request {
        public final ActorRef<LoadBalancer.Mixed> sender;
        public final ActorRef<Customer.Response> ofWhom;

        public State(ActorRef<LoadBalancer.Mixed> sender, ActorRef<Customer.Response> ofWhom) {
            this.sender = sender;
            this.ofWhom = ofWhom;
        }
    }

    public static Behavior<Request> create() {
        return Behaviors.setup(CashRegister::new);
    }

    private CashRegister(ActorContext<Request> context) {
        super(context);

    }

    @Override
    public Receive<Request> createReceive() {
        return newReceiveBuilder()
                .onMessage(Recharge.class, this::onRecharge)
                .onMessage(State.class, this::onState)
                .build();
    }

    /**
     * The cash register recharges the balance of the customer.
     *
     * @param request Contains the request for a recharge.
     * @return this
     */
    private Behavior<Request> onRecharge(Recharge request) {
        getContext().getLog().info("Cash register got recharge request from {}", request.sender.path());
        // if the customer is new to the system, we have to add him/her in the database
        if (Arrays.stream(database).noneMatch(x -> ((x != null) && (x.first() == request.sender)))) {
            database[newSlot] = new Pair<>(request.sender, 1);
            request.sender.tell(new Customer.RechargeSuccess(request.sender, database[newSlot].second()));
            newSlot++;
        } else {
            // else find the information of the customer in database
            for (int i = 0; i < database.length; i++) {
                Pair<ActorRef<Customer.Response>, Integer> info = database[i];
                if ((info != null) && (info.first().equals(request.sender))) {
                    info = new Pair<>(info.first(), info.second() + 1);
                    database[i] = info;
                    // cash register sends a message with the new balance
                    request.sender.tell(new Customer.RechargeSuccess(info.first(), info.second()));
                    break;
                }
            }
        }
        return this;
    }

    /**
     * The cash register gives the balance status of the customer to load balancer
     *
     * @param request Request for money state from customer
     * @return this
     */
    private Behavior<Request> onState(State request) {
        if (Arrays.stream(database).anyMatch((x -> (x != null) && (x.first() == request.ofWhom)))) {
            for (int i = 0; i < database.length; i++) {
                Pair<ActorRef<Customer.Response>, Integer> info = database[i];
                if ((info != null) && (info.first() == request.ofWhom)) {
                    if (info.second() > 0) {
                        // after confirming that the customer has enough money, credit is then decremented
                        info = new Pair<>(request.ofWhom, info.second() - 1);
                        database[i] = info;
                        request.sender.tell(new LoadBalancer.CreditSuccess(this.getContext().getSelf(), request.ofWhom));
                    } else {
                        request.sender.tell(new LoadBalancer.CreditFail(this.getContext().getSelf(), request.ofWhom));
                    }
                }
            }
        } else {
            request.sender.tell(new LoadBalancer.CreditFail(this.getContext().getSelf(), request.ofWhom));
        }
        return this;
    }
}