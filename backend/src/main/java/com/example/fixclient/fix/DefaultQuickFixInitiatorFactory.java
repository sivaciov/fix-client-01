package com.example.fixclient.fix;

import org.springframework.stereotype.Component;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;

@Component
public class DefaultQuickFixInitiatorFactory implements QuickFixInitiatorFactory {

    private final QuickFixExecutionApplication application;

    public DefaultQuickFixInitiatorFactory(QuickFixExecutionApplication application) {
        this.application = application;
    }

    @Override
    public QuickFixInitiator create(SessionSettings settings) throws ConfigError {
        SocketInitiator socketInitiator = new SocketInitiator(
                application,
                new FileStoreFactory(settings),
                settings,
                new FileLogFactory(settings),
                new DefaultMessageFactory()
        );

        return new QuickFixInitiator() {
            @Override
            public void start() throws ConfigError {
                socketInitiator.start();
            }

            @Override
            public void stop() {
                socketInitiator.stop();
            }
        };
    }
}
