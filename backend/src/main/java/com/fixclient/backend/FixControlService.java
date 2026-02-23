package com.fixclient.backend;

import com.example.fixclient.fix.InitiatorStatus;

public interface FixControlService {

    void start();

    void stop();

    InitiatorStatus getStatus();
}
