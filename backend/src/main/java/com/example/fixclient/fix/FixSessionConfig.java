package com.example.fixclient.fix;

public record FixSessionConfig(String senderCompId, String targetCompId, String host, Integer port) {

    public static FixSessionConfig empty() {
        return new FixSessionConfig("", "", "", null);
    }
}
