package com.chessapp.server.data.model.enums;

public enum TimeControl {
    BULLET(60000),    // 1 minute
    BLITZ(180000),    // 3 minutes in test 3 zero in production 4
    RAPID(600000);    // 10 minutes

    private final int milliseconds;

    TimeControl(int milliseconds) {
        this.milliseconds = milliseconds;
    }

    public int getMilliseconds() {
        return milliseconds;
    }
}