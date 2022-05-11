package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class CoffeeMachine extends AbstractBehavior<CoffeeMachine.Request> {
    private int coffee;

    public interface Request {}
    public static final class Put implements Request{
        public ActorRef<Customer.Response> sender;
        public Put(ActorRef<Customer.Response> sender) {
            this.sender = sender;
        }
    }
    public static final class Get implements Request{
        public final ActorRef<Customer.Response> sender;
        public Get(ActorRef<Customer.Response> sender) {
            this.sender = sender;
        }
    }

    public static Behavior<Request> create(int coffee) {
        return Behaviors.setup(context -> new CoffeeMachine(context, coffee));
    }

    private CoffeeMachine(ActorContext<Request> context, int coffee) {
        super(context);
        this.coffee = coffee;
    }

    @Override
    public Receive<Request> createReceive() {
        return newReceiveBuilder()
                .onMessage(Put.class, this::onPut)
                .onMessage(Get.class, this::onGet)
                .build();
    }

    private Behavior<Request> onPut(Put request) {
        getContext().getLog().info("Got a put request from {} ({})!", request.sender.path(), coffee);
        this.coffee += 1;
        request.sender.tell(new Customer.Success());
        return this;
    }

    private Behavior<Request> onGet(Get request) {
        getContext().getLog().info("Got a get request from {} ({})!", request.sender.path(), coffee);
        if (this.coffee > 0) {
            this.coffee -= 1;
            request.sender.tell(new Customer.Success());
        } else {
            request.sender.tell(new Customer.Fail());
        }
        return this;
    }
}
