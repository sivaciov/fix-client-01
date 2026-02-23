package com.example.fixclient.fix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fixclient.backend.execution.ExecutionReportIngestionService;
import com.fixclient.backend.execution.ExecutionReportMapper;
import com.fixclient.backend.execution.ExecutionReportStateStore;
import com.fixclient.backend.orders.InMemoryOrderStore;
import com.fixclient.backend.orders.OrderService;
import org.junit.jupiter.api.Test;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.field.ClOrdID;
import quickfix.field.MsgType;

class QuickFixExecutionApplicationTest {

    private static final OrderSender ACCEPTING_SENDER = submission -> new OrderSendResult(true, "accepted");

    @Test
    void forwardsExecutionReportMessageToIngestionService() throws Exception {
        ExecutionReportStateStore stateStore = new ExecutionReportStateStore();
        OrderService orderService = new OrderService(ACCEPTING_SENDER, new InMemoryOrderStore());
        ExecutionReportIngestionService service =
                new ExecutionReportIngestionService(new ExecutionReportMapper(), stateStore, orderService);
        QuickFixExecutionApplication app = new QuickFixExecutionApplication(service);

        Message message = new Message();
        message.getHeader().setString(MsgType.FIELD, MsgType.EXECUTION_REPORT);
        message.setString(ClOrdID.FIELD, "cl-1");

        app.fromApp(message, new SessionID("FIX.4.4:SENDER->TARGET"));

        assertEquals(1, stateStore.recentReports().size());
        assertNotNull(stateStore.latestFor("cl-1"));
    }

    @Test
    void ignoresNonExecutionReportMessages() throws Exception {
        ExecutionReportStateStore stateStore = new ExecutionReportStateStore();
        OrderService orderService = new OrderService(ACCEPTING_SENDER, new InMemoryOrderStore());
        ExecutionReportIngestionService service =
                new ExecutionReportIngestionService(new ExecutionReportMapper(), stateStore, orderService);
        QuickFixExecutionApplication app = new QuickFixExecutionApplication(service);

        Message message = new Message();
        message.getHeader().setString(MsgType.FIELD, MsgType.ORDER_SINGLE);
        message.setString(ClOrdID.FIELD, "cl-2");

        app.fromApp(message, new SessionID("FIX.4.4:SENDER->TARGET"));

        assertEquals(0, stateStore.recentReports().size());
        assertNull(stateStore.latestFor("cl-2"));
    }
}
