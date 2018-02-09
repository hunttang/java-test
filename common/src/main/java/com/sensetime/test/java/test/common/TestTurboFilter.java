package com.sensetime.test.java.test.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

/**
 * Created by Hunt Tang <tangmingming@xiaomi.com> on 2/2/18.
 */
public class TestTurboFilter extends TurboFilter {
    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if (level == Level.WARN) {
        }
        else if (level == Level.ERROR) {
        }
        return FilterReply.NEUTRAL;
    }
}
