package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class CoffeeMachine extends AbstractBehavior<CoffeeMachine.Request> {
    private int remainingCoffee;

    public interface Request {
    }

    // is triggered when the load balancer asks for
    // the amount of remaining coffee at this coffee machine
    public static final class GiveSupply implements Request {
        public ActorRef<LoadBalancer.Mixed> sender;
        public ActorRef<Customer.Response> ofWhom;
        public ActorRef<Request> coffeeMachine;

        public GiveSupply(ActorRef<LoadBalancer.Mixed> sender, ActorRef<Customer.Response> ofWhom, ActorRef<Request> coffeeMachine) {
            this.sender = sender;
            this.ofWhom = ofWhom;
            this.coffeeMachine = coffeeMachine;
        }
    }

    // is triggered when the customer asks this coffee machine
    // directly for a coffee (after being checked)
    public static final class GetCoffee implements Request {
        public final ActorRef<Customer.Response> sender;

        public GetCoffee(ActorRef<Customer.Response> sender) {
            this.sender = sender;
        }
    }

    public static Behavior<Request> create(int remainingCoffee) {
        return Behaviors.setup(context -> new CoffeeMachine(context, remainingCoffee));
    }

    private CoffeeMachine(ActorContext<Request> context, int remainingCoffee) {
        super(context);
        this.remainingCoffee = remainingCoffee;
    }

    @Override
    public Receive<Request> createReceive() {
        return newReceiveBuilder()
                .onMessage(GiveSupply.class, this::onGiveSupply)
                .onMessage(GetCoffee.class, this::onGetCoffee)
                .build();
    }

    // this coffee machine tells load balancer the remaining supply
    private Behavior<Request> onGiveSupply(GiveSupply response) {
        getContext().getLog().info("{} got a supply request from {} (remaining coffee: {})",
                this.getContext().getSelf().path(), response.sender.path(), remainingCoffee);
        response.sender.tell(new LoadBalancer.GetSupply(this.getContext().getSelf(), response.ofWhom, this.remainingCoffee));
        return this;
    }

    // this coffee machine reacts to the customer,
    // who asks it directly for coffee
    private Behavior<Request> onGetCoffee(GetCoffee request) {
        getContext().getLog().info("{} got a get request from {} (remaining coffee: {})", this.getContext().getSelf(), request.sender.path(), remainingCoffee);
        if (this.remainingCoffee > 0) {
            this.remainingCoffee -= 1;
            request.sender.tell(new Customer.GetSuccess(request.sender));
        } else {
            // the machine runs out of coffee
            request.sender.tell(new Customer.GetFail(request.sender));
        }
        return this;
    }
}
