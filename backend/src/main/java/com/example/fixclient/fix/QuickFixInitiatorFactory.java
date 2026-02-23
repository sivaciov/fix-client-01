package com.example.fixclient.fix;

import quickfix.ConfigError;
import quickfix.SessionSettings;

public interface QuickFixInitiatorFactory {
    QuickFixInitiator create(SessionSettings settings) throws ConfigError;
}
