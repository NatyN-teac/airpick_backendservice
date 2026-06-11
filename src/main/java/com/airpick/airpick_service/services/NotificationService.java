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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PushNotificationService pushNotificationService;
    private final com.airpick.airpick_service.repositories.UserRepository userRepository;

    // -------------------------------------------------------------------------
    // Account
    // -------------------------------------------------------------------------

    @Transactional
    public void notifyWelcome(User user) {
        createAndSend(
                user,
                NotificationType.WELCOME,
                "Welcome to Airpick",
                "Your account is ready. Browse offers, send requests, and start shipping with confidence.",
                RefType.USER,
                user.getId()
        );
    }

    // -------------------------------------------------------------------------
    // Match lifecycle notifications
    // -------------------------------------------------------------------------

    @Transactional
    public void notifyMatchCreated(Match match) {
        createAndSend(
                match.getCarrier(),
                NotificationType.MATCH_CREATED,
                "New Match Request",
                "A shipper has requested to match your offer. Review and accept or reject the request.",
                RefType.MATCH,
                match.getId()
        );
    }

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

    @Transactional
    public void notifyMatchInProgress(Match match) {
        createAndSend(
                match.getShipper(),
                NotificationType.MATCH_IN_PROGRESS,
                "Delivery Started",
                "The carrier has picked up your items and started delivery.",
                RefType.MATCH,
                match.getId()
        );
    }

    @Transactional
    public void notifyMatchDelivered(Match match) {
        createAndSend(
                match.getShipper(),
                NotificationType.MATCH_DELIVERED,
                "Delivery Complete",
                "Your items have been delivered. Thank you for using Airpick.",
                RefType.MATCH,
                match.getId()
        );
    }

    // -------------------------------------------------------------------------
    // Offer lifecycle notifications
    // -------------------------------------------------------------------------

    @Transactional
    public void notifyOfferFullyMatched(User carrier, Offer offer) {
        createAndSend(
                carrier,
                NotificationType.OFFER_FULLY_MATCHED,
                "Offer Fully Matched",
                "All slots on your offer have been matched. No remaining capacity is available.",
                RefType.OFFER,
                offer.getId()
        );
    }

    @Transactional
    public void notifyOfferCancelled(User shipper, Offer offer) {
        createAndSend(
                shipper,
                NotificationType.OFFER_CANCELLED,
                "Offer Cancelled",
                "The carrier has cancelled the offer you matched with. Your pending match has been affected.",
                RefType.OFFER,
                offer.getId()
        );
    }

    @Transactional
    public void notifyOfferRequestCancelled(User carrier, OfferRequest request) {
        createAndSend(
                carrier,
                NotificationType.OFFER_REQUEST_CANCELLED,
                "Request Cancelled",
                "The sender has cancelled an offer request you submitted a proposal on.",
                RefType.OFFER_REQUEST,
                request.getId()
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

    @Transactional
    public void notifyProposalRejected(OfferProposal proposal) {
        createAndSend(
                proposal.getCarrier(),
                NotificationType.PROPOSAL_REJECTED,
                "Proposal Declined",
                "The sender has declined your proposal on their offer request.",
                RefType.PROPOSAL,
                proposal.getId()
        );
    }

    @Transactional
    public void notifyProposalWithdrawn(OfferProposal proposal) {
        createAndSend(
                proposal.getOfferRequest().getShipper(),
                NotificationType.PROPOSAL_WITHDRAWN,
                "Proposal Withdrawn",
                "A carrier has withdrawn their proposal on your offer request.",
                RefType.OFFER_REQUEST,
                proposal.getOfferRequest().getId()
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

        try {
            pushNotificationService.sendToUser(recipient.getId(), fcmData);
        } catch (Exception e) {
            log.error("FCM push failed for notification {} (user {}) — notification is persisted, push skipped: {}",
                    saved.getId(), recipient.getId(), e.getMessage(), e);
        }
    }

    /**
     * Send a system notification to a list of user IDs.
     */
    @Transactional
    public void sendSystemNotificationToUsers(java.util.List<java.util.UUID> userIds,
                                              String title,
                                              String body,
                                              java.util.Map<String, String> data) {
        for (java.util.UUID id : userIds) {
            userRepository.findById(id).ifPresent(u -> createAndSend(u, NotificationType.ADMIN_BROADCAST, title, body, null, null));
        }
    }

    @Transactional
    public void sendSystemNotificationToRole(String roleName, String title, String body, java.util.Map<String, String> data) {
        var users = userRepository.findByRole_Name(roleName);
        for (User u : users) createAndSend(u, NotificationType.ADMIN_BROADCAST, title, body, null, null);
    }

    @Transactional
    public void sendSystemNotificationToAll(String title, String body, java.util.Map<String, String> data) {
        var all = userRepository.findAll();
        for (User u : all) createAndSend(u, NotificationType.ADMIN_BROADCAST, title, body, null, null);
    }
}
