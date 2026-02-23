package com.fixclient.backend.orders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderStore {

    void add(OrderRecord order);

    void update(OrderRecord order);

    Optional<OrderRecord> findByOrderId(UUID orderId);

    Optional<OrderRecord> findByClOrdId(String clOrdId);

    List<OrderRecord> listRecent();
}
