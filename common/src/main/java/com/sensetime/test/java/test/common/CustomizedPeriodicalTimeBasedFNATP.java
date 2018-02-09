package com.sensetime.test.java.test.common;

import ch.qos.logback.core.joran.spi.NoAutoStart;
import ch.qos.logback.core.rolling.DefaultTimeBasedFileNamingAndTriggeringPolicy;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Date;

/**
 * Created by Hunt Tang <tangmingming@xiaomi.com> on 1/26/18.
 */
@NoAutoStart
public final class CustomizedPeriodicalTimeBasedFNATP<E> extends DefaultTimeBasedFileNamingAndTriggeringPolicy<E> {
    private int periodCount;

    public CustomizedPeriodicalTimeBasedFNATP(int periodCount) {
        this.periodCount = periodCount;
    }

    @Override
    public void start() {
        super.start();

        try {
            if (tbrp.getParentsRawFileProperty() != null) {
                File currentFile = new File(tbrp.getParentsRawFileProperty());
                if (currentFile.exists() && currentFile.canRead()) {
                    setDateInCurrentPeriod(new Date(((FileTime)Files.getAttribute(currentFile.toPath(), "creationTime")).toMillis()));
                }
            }
            addInfo("Amending initial period to " + dateInCurrentPeriod);
            computeNextCheck();
        }
        catch (IOException e) {
            addWarn("Cannot amend initial period due to exception.", e);
        }

        switch (rc.getPeriodicityType()) {
            case TOP_OF_SECOND:
            case TOP_OF_MINUTE:
            case TOP_OF_HOUR:
                break;
            default:
                addWarn("Unsupported periodicity type: " + rc.getPeriodicityType().toString());
        }
    }

    @Override
    protected void computeNextCheck() {
        int elapsedCount = 0;
        DateTime dateOfElapsedPeriod = new DateTime(dateInCurrentPeriod.getTime());
        switch (rc.getPeriodicityType()) {
            case TOP_OF_SECOND:
                elapsedCount = dateOfElapsedPeriod.getSecondOfMinute() % periodCount;
                break;
            case TOP_OF_MINUTE:
                elapsedCount = dateOfElapsedPeriod.getMinuteOfHour() % periodCount;
                break;
            case TOP_OF_HOUR:
                elapsedCount = dateOfElapsedPeriod.getHourOfDay() % periodCount;
                break;
            default:
                addError("Unsupported periodicity type: " + rc.getPeriodicityType().toString());
        }
        nextCheck = rc.getEndOfNextNthPeriod(dateInCurrentPeriod, periodCount - elapsedCount).getTime();
    }
}
