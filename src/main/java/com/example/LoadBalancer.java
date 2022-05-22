// Sewerin Kuss 201346
// Duc Anh Le 230662
// Janis Melon 209928

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
    private ActorRef<CoffeeMachine.Request> coffeeMachineMax;
    private int max = 10;
    private int count = 0;

    public interface Mixed {
    }

    /**
     * Is triggered after cash register confirmed that the customer has enough money
     */
    public static class CreditSuccess implements Mixed {
        public ActorRef<CashRegister.Request> sender;
        public ActorRef<Customer.Response> ofWhom;

        public CreditSuccess(ActorRef<CashRegister.Request> sender, ActorRef<Customer.Response> ofWhom) {
            this.sender = sender;
            this.ofWhom = ofWhom;
        }
    }

    /**
     * Is triggered after cash register confirmed that the customer doesn't have enough money
     */
    public static final class CreditFail implements Mixed {
        public ActorRef<CashRegister.Request> sender;
        public ActorRef<Customer.Response> ofWhom;

        public CreditFail(ActorRef<CashRegister.Request> sender, ActorRef<Customer.Response> ofWhom) {
            this.sender = sender;
            this.ofWhom = ofWhom;
        }
    }

    /**
     * Is triggered after receiving the supply report from a coffee machine
     */
    public static final class GetSupply implements Mixed {
        public ActorRef<CoffeeMachine.Request> sender;
        public ActorRef<Customer.Response> ofWhom;
        public int remainingCoffee;

        public GetSupply(ActorRef<CoffeeMachine.Request> sender,
                         ActorRef<Customer.Response> ofWhom, int remainingCoffee) {
            this.sender = sender;
            this.ofWhom = ofWhom;
            this.remainingCoffee = remainingCoffee;
        }
    }

    /**
     * Is triggered after receiving all the supply reports from 3 coffee machines
     */
    public static final class GotAllSupply implements Mixed {
        public ActorRef<LoadBalancer.Mixed> sender;
        public ActorRef<Customer.Response> ofWhom;

        public GotAllSupply(ActorRef<LoadBalancer.Mixed> sender, ActorRef<Customer.Response> ofWhom) {
            this.sender = sender;
            this.ofWhom = ofWhom;
        }
    }

    /**
     * Is triggered after customer asks load balancer for a coffee
     */
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
                .onMessage(GotAllSupply.class, this::onGotAllSupply)
                .build();
    }

    /**
     * Needs to be triggered when the customer has enough money for a coffee.
     *
     * @param respond Contains the success when enough credit is available
     * @return this
     */
    private Behavior<Mixed> onCreditSuccess(CreditSuccess respond) {
        getContext().getLog().info("{} has enough money for coffee", respond.ofWhom);
        //then the load balancer asks all the coffee machines for their supplies
        for (ActorRef<CoffeeMachine.Request> coffeeMachine : coffeeMachinesList) {
            coffeeMachine.tell(new CoffeeMachine.GiveSupply(this.getContext().getSelf(), respond.ofWhom, coffeeMachine));
        }
        return this;
    }

    /**
     * Triggers when the customer doesn't have enough money for a coffee.
     *
     * @param response Contains response on confirmed not available credit
     * @return this
     */
    private Behavior<Mixed> onCreditFail(CreditFail response) {
        // load balancer forwards the error message to the customer
        response.ofWhom.tell(new Customer.BalanceFail(response.ofWhom));
        return this;
    }

    /**
     * Customer asks load balancer for a coffee.
     *
     * @param request request
     * @return this
     */
    private Behavior<Mixed> onGetCoffee(GetCoffee request) {
        getContext().getLog().info("Load balancer got a get coffee request from {}", request.sender.path());
        // load balancer asks cash register if the customer has enough money for a coffee
        cashRegister.tell(new CashRegister.State(this.getContext().getSelf(), request.sender));
        return this;
    }

    //
    //

    /**
     * If the load balancer receives all reports from 3 coffee machines,
     * this will find the coffee machine with the most coffee.
     *
     * @param response Contains response to check for available supply from machines
     * @return this
     */
    private Behavior<Mixed> onGetSupply(GetSupply response) {
        count++;
        if (response.remainingCoffee >= max) {
            max = response.remainingCoffee;
            coffeeMachineMax = response.sender;
        }
        if (count == 3) {
            this.getContext().getSelf().tell(new GotAllSupply(this.getContext().getSelf(), response.ofWhom));
            count = 0;
        }
        return this;
    }

    /**
     * Load balancer returns the coffee machine with the most coffee to the customer.
     *
     * @param response Contains response on all 3 machines for supply info
     * @return this
     */
    private Behavior<Mixed> onGotAllSupply(GotAllSupply response) {
        if (max == 0) {
            response.ofWhom.tell(new Customer.GetFail(response.ofWhom));
        } else {
            response.ofWhom.tell(new Customer.GetCoffeeMachine(this.getContext().getSelf(), coffeeMachineMax));
            max--;
        }
        return this;
    }
}