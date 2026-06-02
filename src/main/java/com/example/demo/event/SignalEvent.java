package com.example.demo.event;

import com.example.demo.data.entity.StatisticalEntity;

public class SignalEvent {
    private final String type;   // STRONG / MEDIUM / WEAK
    private final StatisticalEntity entity;
    private final String nextAction;

    public SignalEvent(String type,
                       StatisticalEntity entity,
                       String nextAction) {
        this.type = type;
        this.entity = entity;
        this.nextAction = nextAction;
    }

    public String getType() { return type; }
    public StatisticalEntity getEntity() { return entity; }
    public String getNextAction() { return nextAction; }
}
