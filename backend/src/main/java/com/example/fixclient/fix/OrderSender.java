package com.example.fixclient.fix;

public interface OrderSender {

    OrderSendResult send(OrderSubmission submission);
}
