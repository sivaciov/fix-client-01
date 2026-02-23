package com.fixclient.backend.orders;

import java.util.List;

public interface OrderStore {

    void add(OrderRecord order);

    List<OrderRecord> listRecent();
}
