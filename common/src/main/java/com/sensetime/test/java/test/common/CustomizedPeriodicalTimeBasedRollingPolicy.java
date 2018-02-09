package com.sensetime.test.java.test.common;

import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by Hunt Tang <tangmingming@xiaomi.com> on 2/1/18.
 */
public class CustomizedPeriodicalTimeBasedRollingPolicy<E> extends TimeBasedRollingPolicy<E> {
    @Setter
    @Getter
    private int periodCount = 1;

    @Override
    public void start() {
        if (getTimeBasedFileNamingAndTriggeringPolicy() == null) {
            setTimeBasedFileNamingAndTriggeringPolicy(new CustomizedPeriodicalTimeBasedFNATP<>(periodCount));
        }

        setMaxHistory(getMaxHistory() * periodCount);

        super.start();
    }
}
