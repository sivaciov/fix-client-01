package com.example.fixclient.fix;

import com.fixclient.backend.execution.ExecutionReportIngestionService;
import org.springframework.stereotype.Component;
import quickfix.ApplicationAdapter;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;
import quickfix.field.MsgType;

@Component
public class QuickFixExecutionApplication extends ApplicationAdapter {

    private final ExecutionReportIngestionService ingestionService;

    public QuickFixExecutionApplication(ExecutionReportIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Override
    public void fromApp(Message message, SessionID sessionID)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        String msgType = message.getHeader().getString(MsgType.FIELD);
        if (MsgType.EXECUTION_REPORT.equals(msgType)) {
            ingestionService.ingest(message);
        }
    }
}
