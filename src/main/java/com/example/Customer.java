package com.example;

import akka.actor.typed.javadsl.AbstractBehavior;

public class Customer extends AbstractBehavior<Customer.Response> {
    public interface Response extends {}
}
