package com.theone.collector.DTO;

import lombok.Data;

@Data
public class AccurateWatcherMessage {

    private String title;

    private String executionTime;

    private String applicationName;

    private String level;

    private String body;
}
