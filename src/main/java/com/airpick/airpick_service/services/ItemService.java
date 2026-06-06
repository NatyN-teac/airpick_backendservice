package com.airpick.airpick_service.services;

import com.airpick.airpick_service.dtos.input.CreateItemRequestDto;
import com.airpick.airpick_service.dtos.output.ItemResponseDto;
import com.airpick.airpick_service.models.Item;
import com.airpick.airpick_service.repositories.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final UserService userService;

    /**
     * Returns all active, approved items ordered by category then name.
     */
    public List<ItemResponseDto> getAllItems() {
        return itemRepository.findAllActiveApproved()
                .stream()
                .map(ItemResponseDto::from)
                .toList();
    }

    /**
     * Returns all items pending admin approval.
     */
    public List<ItemResponseDto> getPendingItems() {
        return itemRepository.findAllPendingApproval()
                .stream()
                .map(ItemResponseDto::from)
                .toList();
    }

    /**
     * Carrier submits a custom item for admin approval.
     * Item starts as isApproved=false and is not visible in the catalogue until approved.
     */
    @Transactional
    public ItemResponseDto submitItem(String email, CreateItemRequestDto dto) {
        log.info("User {} submitting new item: {}", email, dto.name());

        Item item = Item.builder()
                .name(dto.name())
                .category(dto.category())
                .measurementType(dto.measurementType())
                .measurementUnit(dto.measurementUnit())
                .isActive(true)
                .isApproved(false)
                .createdBy(userService.findByEmail(email))
                .build();

        Item saved = itemRepository.save(item);
        log.info("Item submitted with id: {}, pending approval", saved.getId());
        return ItemResponseDto.from(saved);
    }

    /**
     * Admin approves a pending item — it becomes visible in the catalogue.
     */
    @Transactional
    public ItemResponseDto approveItem(UUID itemId) {
        Item item = findById(itemId);

        if (item.isApproved()) {
            throw new IllegalStateException("Item is already approved");
        }

        item.setApproved(true);
        log.info("Item {} approved", itemId);
        return ItemResponseDto.from(itemRepository.save(item));
    }

    /**
     * Admin rejects a pending item — soft-deletes it by marking inactive.
     */
    @Transactional
    public void rejectItem(UUID itemId) {
        Item item = findById(itemId);

        if (item.isApproved()) {
            throw new IllegalStateException("Cannot reject an already approved item");
        }

        item.setActive(false);
        itemRepository.save(item);
        log.info("Item {} rejected and deactivated", itemId);
    }

    private Item findById(UUID itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
    }
}
