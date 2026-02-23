package com.example.fixclient.fix;

import com.fixclient.backend.orders.OrderSide;
import com.fixclient.backend.orders.OrderType;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;
import quickfix.Message;
import quickfix.field.ClOrdID;
import quickfix.field.MsgType;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TimeInForce;
import quickfix.field.TransactTime;

@Component
public class QuickFixOrderMessageFactory {

    public Message build(OrderSubmission submission) {
        Message message = new Message();
        message.getHeader().setString(MsgType.FIELD, MsgType.ORDER_SINGLE);
        message.setString(ClOrdID.FIELD, submission.orderId().toString());
        message.setString(Symbol.FIELD, submission.symbol());
        message.setChar(Side.FIELD, toFixSide(submission.side()));
        message.setDouble(OrderQty.FIELD, submission.qty());
        message.setChar(OrdType.FIELD, toFixOrdType(submission.type()));
        message.setField(new TransactTime(LocalDateTime.ofInstant(submission.createdAt(), ZoneOffset.UTC)));
        message.setChar(TimeInForce.FIELD, toFixTimeInForce(submission.tif()));

        if (submission.type() == OrderType.LIMIT && submission.price() != null) {
            message.setDouble(Price.FIELD, submission.price().doubleValue());
        }

        return message;
    }

    private char toFixSide(OrderSide side) {
        return side == OrderSide.BUY ? Side.BUY : Side.SELL;
    }

    private char toFixOrdType(OrderType type) {
        return type == OrderType.MARKET ? OrdType.MARKET : OrdType.LIMIT;
    }

    private char toFixTimeInForce(com.fixclient.backend.orders.TimeInForce tif) {
        if (tif == com.fixclient.backend.orders.TimeInForce.IOC) {
            return TimeInForce.IMMEDIATE_OR_CANCEL;
        }
        if (tif == com.fixclient.backend.orders.TimeInForce.GTC) {
            return TimeInForce.GOOD_TILL_CANCEL;
        }
        return TimeInForce.DAY;
    }
}
