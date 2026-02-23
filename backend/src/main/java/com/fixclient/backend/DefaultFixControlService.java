package com.fixclient.backend;

import com.example.fixclient.fix.FixInitiatorService;
import com.example.fixclient.fix.InitiatorStatus;
import org.springframework.stereotype.Service;

@Service
public class DefaultFixControlService implements FixControlService {

    private final FixInitiatorService fixInitiatorService;

    public DefaultFixControlService(FixInitiatorService fixInitiatorService) {
        this.fixInitiatorService = fixInitiatorService;
    }

    @Override
    public void start() {
        fixInitiatorService.start();
    }

    @Override
    public void stop() {
        fixInitiatorService.stop();
    }

    @Override
    public InitiatorStatus getStatus() {
        return InitiatorStatus.valueOf(fixInitiatorService.getStatus().name());
    }
}
