package io.streak.habitflow.domain.activitylog.event;

import java.util.List;

public record ActivitiesRecordedEvent(List<ActivityRecordedEvent> events) {}