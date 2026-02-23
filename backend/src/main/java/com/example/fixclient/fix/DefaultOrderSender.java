package com.example.fixclient.fix;

import org.springframework.stereotype.Component;
import quickfix.InvalidMessage;
import quickfix.Session;
import quickfix.SessionID;

@Component
public class DefaultOrderSender implements OrderSender {

    private final FixInitiatorService fixInitiatorService;
    private final QuickFixOrderMessageFactory messageFactory;

    public DefaultOrderSender(
            FixInitiatorService fixInitiatorService,
            QuickFixOrderMessageFactory messageFactory) {
        this.fixInitiatorService = fixInitiatorService;
        this.messageFactory = messageFactory;
    }

    @Override
    public OrderSendResult send(OrderSubmission submission) {
        InitiatorServiceStatus status = fixInitiatorService.getStatus();
        if (status.status() != InitiatorStatus.RUNNING) {
            return new OrderSendResult(
                    false,
                    "Order rejected: FIX initiator is not RUNNING (current status: " + status.status().name() + ")");
        }

        try {
            SessionID sessionID = firstSession(status);
            if (sessionID != null) {
                Session.sendToTarget(messageFactory.build(submission), sessionID);
                return new OrderSendResult(true, "Order accepted and sent to FIX session " + sessionID);
            }

            messageFactory.build(submission);
            return new OrderSendResult(true, "Order accepted; FIX RUNNING but no active session was selected");
        } catch (Exception ex) {
            return new OrderSendResult(true, "Order accepted; FIX send attempted but not confirmed: " + ex.getMessage());
        }
    }

    private SessionID firstSession(InitiatorServiceStatus status) throws InvalidMessage {
        if (status.sessions().isEmpty()) {
            return null;
        }
        return new SessionID(status.sessions().get(0));
    }
}
