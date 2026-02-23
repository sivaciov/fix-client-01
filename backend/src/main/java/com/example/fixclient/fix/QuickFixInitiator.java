package com.example.fixclient.fix;

import quickfix.ConfigError;
import quickfix.RuntimeError;

public interface QuickFixInitiator {
    void start() throws ConfigError, RuntimeError;

    void stop();
}
