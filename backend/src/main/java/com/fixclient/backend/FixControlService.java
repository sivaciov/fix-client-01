package com.fixclient.backend;

import com.example.fixclient.fix.InitiatorServiceStatus;

public interface FixControlService {

    void start();

    void stop();

    InitiatorServiceStatus getStatus();
}
