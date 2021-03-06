package com.opencredo.concursus.domain.events.history;

import com.opencredo.concursus.domain.events.Event;
import com.opencredo.concursus.domain.events.binding.EventTypeBinding;
import com.opencredo.concursus.domain.events.sourcing.CachedEventSource;
import com.opencredo.concursus.domain.events.sourcing.EventSource;
import com.opencredo.concursus.domain.time.TimeRange;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fetches the event history of one or more aggregates.
 */
public final class EventHistoryFetcher {
    /**
     * Create an {@link EventHistoryFetcher} using the supplied {@link EventTypeBinding}.
     * @param eventTypeBinding The {@link EventTypeBinding} to use.
     * @return The constructed {@link EventHistoryFetcher}.
     */
    public static EventHistoryFetcher using(EventTypeBinding eventTypeBinding) {
        return new EventHistoryFetcher(eventTypeBinding, Comparator.comparing(Event::getEventTimestamp));
    }

    /**
     * Create an {@link EventHistoryFetcher} using the supplied {@link EventTypeBinding} and causal order {@link Comparator}.
     * @param eventTypeBinding The {@link EventTypeBinding} to use.
     * @param causalOrderComparator The causal order {@link Comparator} to use.
     * @return The constructed {@link EventHistoryFetcher}.
     */
    public static EventHistoryFetcher using(EventTypeBinding eventTypeBinding, Comparator<Event> causalOrderComparator) {
        return new EventHistoryFetcher(eventTypeBinding, causalOrderComparator);
    }

    private final EventTypeBinding eventTypeBinding;
    private final Comparator<Event> causalOrderComparator;

    private EventHistoryFetcher(EventTypeBinding eventTypeBinding, Comparator<Event> causalOrderComparator) {
        this.eventTypeBinding = eventTypeBinding;
        this.causalOrderComparator = causalOrderComparator;
    }

    /**
     * Get the {@link Event} history for a single aggregate.
     * @param eventSource The {@link EventSource} to get the event history from.
     * @param aggregateId The id of the aggregate to get the event history for.
     * @return The retrieved event history.
     */
    public List<Event> getHistory(EventSource eventSource, String aggregateId) {
        return getHistory(eventSource, aggregateId, TimeRange.unbounded());
    }

    /**
     * Get the {@link Event} history for a single aggregate.
     * @param eventSource The {@link EventSource} to get the event history from.
     * @param aggregateId The id of the aggregate to get the event history for.
     * @param timeRange the time range to restrict the event history to.
     * @return The retrieved event history.
     */
    public List<Event> getHistory(EventSource eventSource, String aggregateId, TimeRange timeRange) {
        return eventTypeBinding.replaying(eventSource, aggregateId, timeRange)
                .inAscendingOrder(causalOrderComparator)
                .toList();
    }

    /**
     * Get the {@link Event} history for a single aggregate.
     * @param cachedEventSource The {@link CachedEventSource} to get the event history from.
     * @param aggregateId The id of the aggregate to get the event history for.
     * @return The retrieved event history.
     */
    public List<Event> getHistory(CachedEventSource cachedEventSource, String aggregateId) {
        return getHistory(cachedEventSource, aggregateId, TimeRange.unbounded());
    }

    /**
     * Get the {@link Event} history for a single aggregate.
     * @param cachedEventSource The {@link CachedEventSource} to get the event history from.
     * @param aggregateId The id of the aggregate to get the event history for.
     * @param timeRange the time range to restrict the event history to.
     * @return The retrieved event history.
     */
    public List<Event> getHistory(CachedEventSource cachedEventSource, String aggregateId, TimeRange timeRange) {
        return eventTypeBinding.replaying(cachedEventSource, aggregateId, timeRange)
                .inAscendingOrder(causalOrderComparator)
                .toList();
    }

    /**
     * Get the {@link Event} histories for multiple aggregates.
     * @param eventSource The {@link EventSource} to get the event histories from.
     * @param aggregateIds The ids of the aggregates to get event histories for.
     * @return The event histories for the requested aggregates, grouped by aggregate id.
     */
    public Map<String, List<Event>> getHistories(EventSource eventSource, Collection<String> aggregateIds) {
        return getHistories(eventSource, aggregateIds, TimeRange.unbounded());
    }


    /**
     * Get the {@link Event} histories for multiple aggregates.
     * @param eventSource The {@link EventSource} to get the event histories from.
     * @param aggregateIds The ids of the aggregates to get event histories for.
     * @param timeRange the time range to restrict the event histories to.
     * @return The event histories for the requested aggregates, grouped by aggregate id.
     */
    public Map<String, List<Event>> getHistories(EventSource eventSource, Collection<String> aggregateIds, TimeRange timeRange) {
        CachedEventSource cachedEventSource = eventTypeBinding.preload(eventSource, aggregateIds, timeRange);
        return aggregateIds.stream().collect(Collectors.toMap(
                Function.identity(),
                id -> getHistory(cachedEventSource, id)
        ));
    }

}
