package com.ps;

import io.vertx.core.Starter;

public class Runner {

    public static void main(String[] args) {
        // To use external configuration add -conf src/main/resources/application-conf.json
        Starter.main(new String[] {"run", "com.ps.VerticleExample"});
    }
}
