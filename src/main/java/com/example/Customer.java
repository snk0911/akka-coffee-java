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

    public interface Response {
    }

    /**
     * Is triggered after balance is successfully recharged.
     */
    public static final class RechargeSuccess implements Response {
        private final ActorRef<Customer.Response> ofWhom;
        private final int balance;

        public RechargeSuccess(ActorRef<Customer.Response> ofWhom, int balance) {
            this.ofWhom = ofWhom;
            this.balance = balance;
        }
    }

    /**
     * Is triggered when load balancer sends a message that balance is not enough for a coffee.
     */
    public static final class BalanceFail implements Response {
        private final ActorRef<Customer.Response> ofWhom;

        public BalanceFail(ActorRef<Customer.Response> ofWhom) {
            this.ofWhom = ofWhom;
        }
    }

    /**
     * Is triggered after load balancer returns the machine with the most remaining coffee.
     */
    public static final class GetCoffeeMachine implements Response {
        public ActorRef<LoadBalancer.Mixed> sender;
        public ActorRef<CoffeeMachine.Request> coffeeMachine;

        public GetCoffeeMachine(ActorRef<LoadBalancer.Mixed> sender, ActorRef<CoffeeMachine.Request> coffeeMachine) {
            this.sender = sender;
            this.coffeeMachine = coffeeMachine;
        }
    }

    /**
     * Is triggered after the customer has received a coffee from the machine.
     */
    public static final class GetSuccess implements Response {
        private final ActorRef<Customer.Response> ofWhom;

        public GetSuccess(ActorRef<Customer.Response> ofWhom) {
            this.ofWhom = ofWhom;
        }
    }

    /**
     * Is triggered when the chosen coffee machine is empty.
     */
    public static final class GetFail implements Response {
        private final ActorRef<Customer.Response> ofWhom;

        public GetFail(ActorRef<Customer.Response> ofWhom) {
            this.ofWhom = ofWhom;
        }
    }


    public static Behavior<Response> create(ActorRef<CashRegister.Request> cashRegister, ActorRef<LoadBalancer.Mixed> loadBalancer) {
        return Behaviors.setup(context -> new Customer(context, loadBalancer, cashRegister));
    }

    private Customer(ActorContext<Response> context, ActorRef<LoadBalancer.Mixed> loadBalancer, ActorRef<CashRegister.Request> cashRegister) {
        super(context);
        this.loadBalancer = loadBalancer;
        this.cashRegister = cashRegister;
        if (Math.random() < 0.5) {
            cashRegister.tell(new CashRegister.Recharge(this.getContext().getSelf()));
        } else {
            loadBalancer.tell(new LoadBalancer.GetCoffee(this.getContext().getSelf()));
        }
    }

    @Override
    public Receive<Response> createReceive() {
        return newReceiveBuilder()
                .onMessage(RechargeSuccess.class, this::onRechargeSuccess)
                .onMessage(BalanceFail.class, this::onBalanceFail)
                .onMessage(GetCoffeeMachine.class, this::onGetCoffeeMachine)
                .onMessage(GetSuccess.class, this::onGetSuccess)
                .onMessage(GetFail.class, this::onGetFail)
                .build();
    }

    /**
     * The cash register confirms that the recharge was successful and shows the new balance.
     *
     * @param response Contains a success/no success response for the tried recharge
     * @return this
     * @throws InterruptedException If sync fails...
     */
    //
    private Behavior<Response> onRechargeSuccess(RechargeSuccess response) throws InterruptedException {
        getContext().getLog().info("{}, you have successfully recharged your balance. Current balance: {}",
                response.ofWhom, response.balance);
        Thread.sleep(2000);
        if (Math.random() < 0.5) {
            cashRegister.tell(new CashRegister.Recharge(this.getContext().getSelf()));
        } else {
            loadBalancer.tell(new LoadBalancer.GetCoffee(this.getContext().getSelf()));
        }
        return this;
    }

    /**
     * The customer doesn't have enough money for a coffee.
     *
     * @param command Used to track from whom the message is
     * @throws InterruptedException If sync fails...
     */
    private Behavior<Response> onBalanceFail(BalanceFail command) throws InterruptedException {
        getContext().getLog().info("{}, your current balance is insufficient for a coffee. Please try again.",
                command.ofWhom);
        Thread.sleep(2000);
        if (Math.random() < 0.5) {
            cashRegister.tell(new CashRegister.Recharge(this.getContext().getSelf()));
        } else {
            loadBalancer.tell(new LoadBalancer.GetCoffee(this.getContext().getSelf()));
        }
        return this;
    }

    /**
     * Customer receives the coffee machine with the most remaining supply
     *
     * @param response Contains response from the machine with the most coffee
     * @return this
     */
    private Behavior<Response> onGetCoffeeMachine(GetCoffeeMachine response) {
        getContext().getLog().info("{}, you can now take coffee from {}",
                this.getContext().getSelf(), response.coffeeMachine.path());
        response.coffeeMachine.tell(new CoffeeMachine.GetCoffee(this.getContext().getSelf()));
        return this;
    }

    /**
     * Customer successfully received a coffee from the coffee machine.
     *
     * @param response Contains a success response
     * @return this
     * @throws InterruptedException If sync fails...
     */
    //
    private Behavior<Response> onGetSuccess(GetSuccess response) throws InterruptedException {
        getContext().getLog().info("Here is your coffee {}!", response.ofWhom);
        Thread.sleep(2000);
        if (Math.random() < 0.5) {
            cashRegister.tell(new CashRegister.Recharge(this.getContext().getSelf()));
        } else {
            loadBalancer.tell(new LoadBalancer.GetCoffee(this.getContext().getSelf()));
        }
        return this;
    }

    /**
     * If the coffee machines ran out of coffee. This service makes everything stop.
     *
     * @param response Contains the fail status
     * @return this
     */
    private Behavior<Response> onGetFail(GetFail response) {
        getContext().getLog().info("Sorry {}, we have run out of coffee. Please try again later.", response.ofWhom);
        return Behaviors.stopped();
    }
}