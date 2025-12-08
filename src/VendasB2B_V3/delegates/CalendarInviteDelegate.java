package com.austa.vendas.delegates;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

/**
 * CalendarInviteDelegate - Creates calendar events and sends invites
 *
 * Purpose: Schedules meetings, demos, and appointments via Google Calendar
 * or Microsoft Outlook, automatically sending invites to participants.
 *
 * Input Variables:
 * - eventTitle: String - Event title
 * - eventDescription: String - Event description
 * - startDateTime: Date - Event start date/time
 * - endDateTime: Date - Event end date/time
 * - attendeeEmails: List<String> - Attendee email addresses
 * - location: String - Meeting location (optional)
 * - videoConferenceLink: String - Video conference link (optional)
 * - organizerEmail: String - Organizer email
 *
 * Output Variables:
 * - calendarInviteSentSuccess: Boolean - Send success indicator
 * - calendarInviteSentTimestamp: Date - Send timestamp
 * - eventId: String - Calendar event ID
 * - eventUrl: String - URL to view event
 *
 * @author AUSTA V3 Backend Team
 * @version 3.0.0
 */
@Component("calendarInviteDelegate")
public class CalendarInviteDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalendarInviteDelegate.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long TIMEOUT_MS = 30000;

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public CalendarInviteDelegate() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slidingWindowSize(10)
            .build();
        this.circuitBreaker = CircuitBreaker.of("calendarInvite", cbConfig);

        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(MAX_RETRY_ATTEMPTS)
            .waitDuration(Duration.ofSeconds(5))
            .retryExceptions(Exception.class)
            .build();
        this.retry = Retry.of("calendarInvite", retryConfig);
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String eventTitle = (String) execution.getVariable("eventTitle");
        Date startDateTime = (Date) execution.getVariable("startDateTime");

        LOGGER.info("Creating calendar invite: title={}, start={}", eventTitle, startDateTime);

        validateInputs(eventTitle, startDateTime);

        try {
            Map<String, Object> inviteResult = circuitBreaker.executeSupplier(() ->
                retry.executeSupplier(() -> createCalendarEvent(execution))
            );

            execution.setVariable("calendarInviteSentSuccess", true);
            execution.setVariable("calendarInviteSentTimestamp", new Date());
            execution.setVariable("eventId", inviteResult.get("eventId"));
            execution.setVariable("eventUrl", inviteResult.get("eventUrl"));

            LOGGER.info("Calendar invite sent successfully: eventId={}", inviteResult.get("eventId"));

        } catch (Exception e) {
            LOGGER.error("Calendar invite failed after {} attempts", MAX_RETRY_ATTEMPTS, e);

            execution.setVariable("calendarInviteSentSuccess", false);
            execution.setVariable("calendarInviteSentError", e.getMessage());
        }
    }

    private void validateInputs(String eventTitle, Date startDateTime) {
        if (eventTitle == null || eventTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("eventTitle is required");
        }
        if (startDateTime == null) {
            throw new IllegalArgumentException("startDateTime is required");
        }
    }

    private Map<String, Object> createCalendarEvent(DelegateExecution execution) throws Exception {
        Map<String, Object> eventPayload = buildEventPayload(execution);

        LOGGER.debug("Creating calendar event: {}", eventPayload);

        // TODO: Implement actual Google Calendar or Outlook API call
        // POST https://www.googleapis.com/calendar/v3/calendars/primary/events

        Thread.sleep(1000); // Simulate API call

        String eventId = "EVT-" + System.currentTimeMillis();
        String eventUrl = "https://calendar.google.com/event?eid=" + eventId;

        Map<String, Object> result = new HashMap<>();
        result.put("eventId", eventId);
        result.put("eventUrl", eventUrl);

        return result;
    }

    private Map<String, Object> buildEventPayload(DelegateExecution execution) {
        Map<String, Object> payload = new HashMap<>();

        String eventTitle = (String) execution.getVariable("eventTitle");
        String eventDescription = (String) execution.getVariable("eventDescription");
        Date startDateTime = (Date) execution.getVariable("startDateTime");
        Date endDateTime = (Date) execution.getVariable("endDateTime");
        List<String> attendeeEmails = (List<String>) execution.getVariable("attendeeEmails");
        String location = (String) execution.getVariable("location");
        String videoConferenceLink = (String) execution.getVariable("videoConferenceLink");

        payload.put("summary", eventTitle);
        payload.put("description", eventDescription);

        // Start time
        Map<String, String> start = new HashMap<>();
        start.put("dateTime", formatDateTime(startDateTime));
        start.put("timeZone", "America/Sao_Paulo");
        payload.put("start", start);

        // End time
        Map<String, String> end = new HashMap<>();
        end.put("dateTime", formatDateTime(endDateTime != null ? endDateTime :
                                         new Date(startDateTime.getTime() + 3600000))); // +1 hour
        end.put("timeZone", "America/Sao_Paulo");
        payload.put("end", end);

        // Attendees
        if (attendeeEmails != null && !attendeeEmails.isEmpty()) {
            List<Map<String, String>> attendees = new ArrayList<>();
            for (String email : attendeeEmails) {
                Map<String, String> attendee = new HashMap<>();
                attendee.put("email", email);
                attendees.add(attendee);
            }
            payload.put("attendees", attendees);
        }

        // Location
        if (location != null) {
            payload.put("location", location);
        }

        // Video conference
        if (videoConferenceLink != null) {
            payload.put("conferenceData", Map.of("entryPoints", Collections.singletonList(
                Map.of("uri", videoConferenceLink, "entryPointType", "video")
            )));
        }

        payload.put("reminders", Map.of(
            "useDefault", false,
            "overrides", Arrays.asList(
                Map.of("method", "email", "minutes", 1440), // 1 day before
                Map.of("method", "popup", "minutes", 30)    // 30 min before
            )
        ));

        return payload;
    }

    private String formatDateTime(Date date) {
        // Format: 2024-12-08T14:30:00-03:00
        return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(date);
    }
}
