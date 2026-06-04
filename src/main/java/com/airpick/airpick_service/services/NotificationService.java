package com.airpick.airpick_service.services;

import com.airpick.airpick_service.models.*;
import com.airpick.airpick_service.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Handles all user notifications — persists them to the DB and pushes via FCM.
 * <p>
 * Every notification is stored in the {@code notifications} table before being sent,
 * so users have a reliable inbox regardless of whether their device received the push.
 * <p>
 * The FCM payload is a data-only message carrying:
 * <ul>
 *   <li>{@code notificationId} — the persisted row ID, for deduplication on the client</li>
 *   <li>{@code type} — the notification type string (e.g. "MATCH_ACCEPTED")</li>
 *   <li>{@code title} / {@code body} — for display in Flutter local notifications</li>
 *   <li>{@code refType} / {@code refId} — the deep-link target (e.g. "MATCH" + matchId)</li>
 * </ul>
 * <p>
 * WebSocket is NOT used here. WebSocket is used exclusively for real-time chat
 * messages in {@link ChatService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PushNotificationService pushNotificationService;

    // -------------------------------------------------------------------------
    // Match lifecycle notifications
    // -------------------------------------------------------------------------

    @Transactional
    public void notifyMatchAccepted(Match match) {
        createAndSend(
                match.getShipper(),
                NotificationType.MATCH_ACCEPTED,
                "Match Accepted",
                "Your match request has been accepted by the carrier. Your chat room is now open.",
                RefType.MATCH,
                match.getId()
        );
    }

    @Transactional
    public void notifyMatchRejected(Match match, String rejectionReason) {
        createAndSend(
                match.getShipper(),
                NotificationType.MATCH_REJECTED,
                "Match Rejected",
                "Your match request was rejected: " + rejectionReason,
                RefType.MATCH,
                match.getId()
        );
    }

    @Transactional
    public void notifyMatchCancelled(Match match, String cancelledByEmail) {
        boolean cancelledByCarrier = match.getCarrier().getEmail().equals(cancelledByEmail);
        User recipient = cancelledByCarrier ? match.getShipper() : match.getCarrier();
        String body = cancelledByCarrier
                ? "The carrier has cancelled this match."
                : "The shipper has cancelled this match.";

        createAndSend(
                recipient,
                NotificationType.MATCH_CANCELLED,
                "Match Cancelled",
                body,
                RefType.MATCH,
                match.getId()
        );
    }

    // -------------------------------------------------------------------------
    // Proposal lifecycle notifications
    // -------------------------------------------------------------------------

    @Transactional
    public void notifyNewProposal(OfferProposal proposal) {
        createAndSend(
                proposal.getOfferRequest().getShipper(),
                NotificationType.NEW_PROPOSAL,
                "New Proposal Received",
                "A carrier has submitted a proposal for your offer request.",
                RefType.OFFER_REQUEST,
                proposal.getOfferRequest().getId()
        );
    }

    @Transactional
    public void notifyProposalAccepted(OfferProposal proposal, UUID matchId) {
        createAndSend(
                proposal.getCarrier(),
                NotificationType.PROPOSAL_ACCEPTED,
                "Proposal Accepted",
                "The sender has accepted your proposal. A match has been created and your chat room is open.",
                RefType.MATCH,
                matchId
        );
    }

    // -------------------------------------------------------------------------
    // Chat notifications
    // -------------------------------------------------------------------------

    @Transactional
    public void notifyNewMessage(Match match, String senderEmail) {
        User recipient = match.getCarrier().getEmail().equals(senderEmail)
                ? match.getShipper()
                : match.getCarrier();

        createAndSend(
                recipient,
                NotificationType.NEW_MESSAGE,
                "New Message",
                "You have a new message.",
                RefType.MATCH,
                match.getId()
        );
    }

    // -------------------------------------------------------------------------
    // Inbox queries (used by NotificationController)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<Notification> getNotificationsForUser(UUID userId, Pageable pageable) {
        return notificationRepository.findAllByRecipientId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countUnreadByRecipientId(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new IllegalArgumentException("Notification does not belong to this user");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        int updated = notificationRepository.markAllReadByRecipientId(userId);
        log.info("Marked {} notifications as read for user {}", updated, userId);
        return updated;
    }

    // -------------------------------------------------------------------------
    // Private core
    // -------------------------------------------------------------------------

    private void createAndSend(User recipient,
                                NotificationType type,
                                String title,
                                String body,
                                RefType refType,
                                UUID refId) {

        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .body(body)
                .isRead(false)
                .refType(refType)
                .refId(refId)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification [{}] persisted for user {}", type, recipient.getId());

        Map<String, String> fcmData = Map.of(
                "notificationId", saved.getId().toString(),
                "type",           type.name(),
                "title",          title,
                "body",           body,
                "refType",        refType != null ? refType.name() : "",
                "refId",          refId != null ? refId.toString() : ""
        );

        // FCM is best-effort — a push failure must never roll back the calling business transaction.
        // The notification is already persisted; the client can fetch it from the inbox on next open.
        try {
            pushNotificationService.sendToUser(recipient.getId(), fcmData);
        } catch (Exception e) {
            log.error("FCM push failed for notification {} (user {}) — notification is persisted, push skipped: {}",
                    saved.getId(), recipient.getId(), e.getMessage(), e);
        }
    }
}
