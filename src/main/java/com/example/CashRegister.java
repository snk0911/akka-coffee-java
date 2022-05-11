package com.example;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class CashRegister extends AbstractBehavior<CashRegister.Request> {

        private int balance;

        public interface Request {}

        public static final class Recharge implements Request {
            public ActorRef<Customer.Response> sender;

            public Recharge(ActorRef<Customer.Response> sender) {
                this.sender = sender;
            }
        }

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

        private Behavior<Request> onRecharge(Recharge request) {
            getContext().getLog().info("Got a deposit request from {} ({})!", request.sender.path(), guthaben);
            this.balance += 1;
            request.sender.tell(new Customer.Success());
            return this;
        }

        private Behavior<Request> onState(State request) {
            getContext().getLog().info("Got a status request from {} ({})!", request.sender.path(), balance);
            if (this.balance > 0) {
                this.balance -= 1;
                request.sender.tell(new LoadBalancer.Success());
            } else {
                request.sender.tell(new LoadBalancer.Fail());
            }
            return this;
        }
    }
}
