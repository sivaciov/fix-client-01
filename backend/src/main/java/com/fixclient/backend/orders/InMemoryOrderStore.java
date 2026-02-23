package com.fixclient.backend.orders;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

@Component
public class InMemoryOrderStore implements OrderStore {

    private final ConcurrentLinkedDeque<OrderRecord> orders = new ConcurrentLinkedDeque<>();

    @Override
    public void add(OrderRecord order) {
        orders.addFirst(order);
    }

    @Override
    public List<OrderRecord> listRecent() {
        return List.copyOf(new ArrayList<>(orders));
    }
}
