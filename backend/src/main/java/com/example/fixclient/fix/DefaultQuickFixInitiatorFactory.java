package com.example.fixclient.fix;

import org.springframework.stereotype.Component;
import quickfix.ApplicationAdapter;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;

@Component
public class DefaultQuickFixInitiatorFactory implements QuickFixInitiatorFactory {

    @Override
    public QuickFixInitiator create(SessionSettings settings) throws ConfigError {
        SocketInitiator socketInitiator = new SocketInitiator(
                new ApplicationAdapter(),
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
