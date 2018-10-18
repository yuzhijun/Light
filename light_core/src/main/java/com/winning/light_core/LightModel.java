package com.winning.light_core;

public class LightModel {
    enum Action {
        WRITE, SEND, FLUSH
    }

    Action action;
    WriteAction writeAction;
    SendAction sendAction;
    FlushAction flushAction;

    boolean isValid() {
        boolean valid = false;
        if (action != null) {
            if (action == Action.SEND && sendAction != null && sendAction.isValid()) {
                valid = true;
            } else if (action == Action.WRITE && writeAction != null && writeAction.isValid()) {
                valid = true;
            } else if (action == Action.FLUSH && flushAction != null) {
                valid = true;
            }
        }
        return valid;
    }
}
