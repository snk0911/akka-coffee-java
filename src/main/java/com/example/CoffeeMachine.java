package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

public class CoffeeMachine extends AbstractBehavior<CoffeeMachine.Request> {
    private int remainingCoffee;

    public interface Request extends LoadBalancer.Mixed {
    }

    // is triggered when the load balancer asks for the amount of remaining coffee at each machine
    public static final class GiveSupply implements Request {
        public ActorRef<LoadBalancer.Mixed> sender;
        public ActorRef<CoffeeMachine.Request> coffeeMachine;

        public GiveSupply(ActorRef<LoadBalancer.Mixed> sender, ActorRef<CoffeeMachine.Request> coffeeMachine) {
            this.sender = sender;
            this.coffeeMachine = coffeeMachine;
        }
    }

    // is triggered when the customer asks direct for a coffee (after being checked)
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

    // the machine tells load balancer the remaining supply
    private Behavior<Request> onGiveSupply(GiveSupply request) {
        getContext().getLog().info("Got a supply request from {} (remaining coffee: {})!", request.sender.path(), remainingCoffee);
        request.sender.tell(new LoadBalancer.GetSupply(this.getContext().getSelf(), this.remainingCoffee));
        return this;
    }

    // machine reacts the customer asks for coffee directly at this coffee machine
    private Behavior<Request> onGetCoffee(GetCoffee request) {
        getContext().getLog().info("Got a get request from {} ({})!", request.sender.path(), remainingCoffee);
        if (this.remainingCoffee > 0) {
            this.remainingCoffee -= 1;
            request.sender.tell(new Customer.GetSuccess());
        } else {
            // the machine runs out of coffee
            request.sender.tell(new Customer.GetFail());
        }
        return this;
    }
}
