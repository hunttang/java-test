package com.sensetime.test.java.test.light4j;

import com.networknt.server.StartupHookProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Hunt Tang <tangmingming@xiaomi.com> on 2/6/18.
 */
public class ServiceStartupHookProvider implements StartupHookProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceStartupHookProvider.class);

    public void onStartup() {
        LOGGER.info("ServiceStartupHookProvider called.");
    }
}
