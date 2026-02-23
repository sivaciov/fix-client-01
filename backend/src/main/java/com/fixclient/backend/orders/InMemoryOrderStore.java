package com.fixclient.backend.orders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

@Component
public class InMemoryOrderStore implements OrderStore {

    private final ConcurrentHashMap<UUID, OrderRecord> byOrderId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UUID> clOrdIdToOrderId = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<UUID> orderSequence = new ConcurrentLinkedDeque<>();

    @Override
    public synchronized void add(OrderRecord order) {
        byOrderId.put(order.orderId(), order);
        clOrdIdToOrderId.put(order.clOrdId(), order.orderId());
        orderSequence.addFirst(order.orderId());
    }

    @Override
    public synchronized void update(OrderRecord order) {
        byOrderId.put(order.orderId(), order);
    }

    @Override
    public Optional<OrderRecord> findByOrderId(UUID orderId) {
        return Optional.ofNullable(byOrderId.get(orderId));
    }

    @Override
    public Optional<OrderRecord> findByClOrdId(String clOrdId) {
        UUID orderId = clOrdIdToOrderId.get(clOrdId);
        return orderId == null ? Optional.empty() : findByOrderId(orderId);
    }

    @Override
    public List<OrderRecord> listRecent() {
        List<OrderRecord> orders = new ArrayList<>();
        for (UUID orderId : orderSequence) {
            OrderRecord order = byOrderId.get(orderId);
            if (order != null) {
                orders.add(order);
            }
        }
        return List.copyOf(orders);
    }
}
