package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public ItemDto createItem(ItemDto itemDto, Long userId) {
        User owner = findUserOrThrow(userId);
        Item item = ItemMapper.toItem(itemDto);
        item.setOwner(owner);
        if (itemDto.getRequestId() != null) {
            item.setRequestId(itemDto.getRequestId());
        }
        Item savedItem = itemRepository.save(item);
        return ItemMapper.toItemDto(savedItem);
    }

    @Override
    @Transactional
    public ItemDto updateItem(Long itemId, ItemDto itemDto, Long userId) {
        Item existingItem = findItemOrThrow(itemId);
        checkItemOwnership(existingItem, userId);
        if (itemDto.getName() != null) {
            existingItem.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null) {
            existingItem.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            existingItem.setAvailable(itemDto.getAvailable());
        }
        Item updatedItem = itemRepository.save(existingItem);
        return ItemMapper.toItemDto(updatedItem);
    }

    @Override
    public ItemWithBookingsDto getItemById(Long itemId, Long userId) {
        Item item = findItemOrThrow(itemId);
        ItemWithBookingsDto itemWithBookings = ItemMapper.toItemWithBookingsDto(item);
        if (item.getOwner().getId().equals(userId)) {
            addBookingInfoToItem(itemWithBookings, itemId);
        }
        addCommentsToItem(itemWithBookings, itemId);
        return itemWithBookings;
    }

    @Override
    public List<ItemWithBookingsDto> getAllUserItems(Long userId) {
        findUserOrThrow(userId);
        List<Item> items = itemRepository.findAllByOwnerId(userId);
        List<Long> itemIds = items.stream().map(Item::getId).collect(Collectors.toList());
        Map<Long, List<CommentDto>> commentsByItemId = getCommentsByItemIds(itemIds);
        LocalDateTime now = LocalDateTime.now();
        List<Booking> allLastBookings = bookingRepository.findLastBookingsForItems(itemIds, now);
        List<Booking> allNextBookings = bookingRepository.findNextBookingsForItems(itemIds, now);
        Map<Long, Booking> lastBookingMap = allLastBookings.stream()
                .collect(Collectors.toMap(booking -> booking.getItem().getId(), booking -> booking));
        Map<Long, Booking> nextBookingMap = allNextBookings.stream()
                .collect(Collectors.toMap(booking -> booking.getItem().getId(), booking -> booking));
        return items.stream()
                .map(item -> {
                    ItemWithBookingsDto itemWithBookings = ItemMapper.toItemWithBookingsDto(item);
                    if (lastBookingMap.containsKey(item.getId())) {
                        itemWithBookings.setLastBooking(BookingMapper.toDto(lastBookingMap.get(item.getId())));
                    }
                    if (nextBookingMap.containsKey(item.getId())) {
                        itemWithBookings.setNextBooking(BookingMapper.toDto(nextBookingMap.get(item.getId())));
                    }
                    itemWithBookings.setComments(commentsByItemId.getOrDefault(item.getId(), List.of()));
                    return itemWithBookings;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemDto> searchItems(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        return itemRepository.searchAvailableItems(text).stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteItem(Long itemId, Long userId) {
        Item item = findItemOrThrow(itemId);
        checkItemOwnership(item, userId);
        itemRepository.deleteById(itemId);
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));
    }

    private Item findItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с ID " + itemId + " не найдена"));
    }

    private void checkItemOwnership(Item item, Long userId) {
        if (!item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Только владелец может выполнить это действие");
        }
    }

    private void addBookingInfoToItem(ItemWithBookingsDto itemDto, Long itemId) {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> lastBookings = bookingRepository.findLastBooking(itemId, now);
        if (!lastBookings.isEmpty()) {
            itemDto.setLastBooking(BookingMapper.toDto(lastBookings.get(0)));
        }
        List<Booking> nextBookings = bookingRepository.findNextBooking(itemId, now);
        if (!nextBookings.isEmpty()) {
            itemDto.setNextBooking(BookingMapper.toDto(nextBookings.get(0)));
        }
    }

    @Override
    @Transactional
    public CommentDto addComment(Long itemId, CommentDto commentDto, Long userId) {
        Item item = findItemOrThrow(itemId);
        User author = findUserOrThrow(userId);
        if (!hasUserBookedItem(itemId, userId)) {
            throw new ValidationException("Пользователь " + userId + " не брал эту вещь " + itemId + " в аренду " + LocalDateTime.now());
        }
        Comment comment = new Comment();
        comment.setText(commentDto.getText());
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);
        return CommentMapper.toDto(savedComment);
    }

    private boolean hasUserBookedItem(Long itemId, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return bookingRepository.findByBookerIdAndItemIdAndEndBefore(userId, itemId, now)
                .stream()
                .anyMatch(booking -> booking.getStatus() == BookingStatus.APPROVED);
    }

    private void addCommentsToItem(ItemWithBookingsDto itemDto, Long itemId) {
        List<Comment> comments = commentRepository.findByItemId(itemId);
        List<CommentDto> commentDtos = comments.stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
        itemDto.setComments(commentDtos);
    }

    private Map<Long, List<CommentDto>> getCommentsByItemIds(List<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        return commentRepository.findByItemIdIn(itemIds)
                .stream()
                .collect(Collectors.groupingBy(
                        comment -> comment.getItem().getId(),
                        Collectors.mapping(CommentMapper::toDto, Collectors.toList())
                ));
    }

    @Override
    public List<ItemDto> getItemsByRequestId(Long requestId) {
        List<Item> items = itemRepository.findAllByRequestIdInWithOwner(List.of(requestId));
        return items.stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, List<ItemDto>> getItemsByRequestIds(List<Long> requestIds) {
        if (requestIds.isEmpty()) {
            return Map.of();
        }
        List<Item> items = itemRepository.findAllByRequestIdInWithOwner(requestIds);
        return items.stream()
                .collect(Collectors.groupingBy(
                        Item::getRequestId,
                        Collectors.mapping(ItemMapper::toItemDto, Collectors.toList())
                ));
    }
}