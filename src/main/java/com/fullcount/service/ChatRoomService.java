package com.fullcount.service;

import com.fullcount.domain.ChatMessage;
import com.fullcount.domain.ChatRoom;
import com.fullcount.domain.ChatRoomParticipant;
import com.fullcount.domain.ChatRoomType;
import com.fullcount.domain.CrewParticipant;
import com.fullcount.domain.Member;
import com.fullcount.domain.Post;
import com.fullcount.dto.ChatDTO;
import com.fullcount.dto.common.CursorResponse;
import com.fullcount.dto.common.PagedResponse;
import com.fullcount.exception.BusinessException;
import com.fullcount.exception.ErrorCode;
import com.fullcount.mapper.ChatMapper;
import com.fullcount.mapper.ChatRoomMapper;
import com.fullcount.repository.ChatMessageRepository;
import com.fullcount.repository.ChatRoomRepository;
import com.fullcount.repository.MemberRepository;
import com.fullcount.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    public PagedResponse<ChatDTO.ChatRoomResponse> getMyChatRooms(Long memberId, Pageable pageable) {
        log.info("채팅방 목록 조회 시작 - memberId={}, page={}, size={}", memberId, pageable.getPageNumber(), pageable.getPageSize());

        Page<ChatRoom> chatRooms = chatRoomRepository.findAllByMemberId(memberId, pageable);

        List<Long> roomIds = chatRooms.map(ChatRoom::getId).toList();
        Map<Long, ChatMessage> lastMessageMap = chatMessageRepository.findLastMessagesByRoomIds(roomIds)
                .stream()
                .collect(Collectors.toMap(message -> message.getChatRoom().getId(), message -> message));

        Page<ChatDTO.ChatRoomResponse> responsePage = chatRooms.map(room ->
                ChatMapper.toChatRoomResponse(room, lastMessageMap.get(room.getId())));

        log.info("채팅방 목록 조회 완료 - memberId={}, total={}", memberId, chatRooms.getTotalElements());
        return PagedResponse.of(responsePage);
    }

    public ChatDTO.ChatRoomDetailResponse getChatRoomDetail(Long memberId, Long roomId) {
        log.info("채팅방 상세 조회 시작 - memberId={}, roomId={}", memberId, roomId);

        ChatRoom chatRoom = chatRoomRepository.findByIdWithDetails(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        List<Member> participants = getParticipants(chatRoom);
        boolean isParticipant = participants.stream().anyMatch(member -> member.getId().equals(memberId));
        if (!isParticipant) {
            log.warn("채팅방 상세 조회 접근 거부 - memberId={}, roomId={}", memberId, roomId);
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        log.info("채팅방 상세 조회 완료 - memberId={}, roomId={}, 참여자 수={}", memberId, roomId, participants.size());
        return ChatMapper.toChatRoomDetailResponse(chatRoom, participants);
    }

    public CursorResponse<ChatDTO.ChatMessagePayload> getChatMessages(Long memberId, Long roomId, Long lastMessageId, int size) {
        log.info("채팅 메시지 조회 시작 - memberId={}, roomId={}, lastMessageId={}, size={}", memberId, roomId, lastMessageId, size);

        if (!chatRoomRepository.existsById(roomId)) {
            log.warn("채팅 메시지 조회 실패 - 채팅방 없음, roomId={}", roomId);
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }

        if (!chatRoomRepository.isParticipant(roomId, memberId)) {
            log.warn("채팅 메시지 조회 접근 거부 - memberId={}, roomId={}", memberId, roomId);
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        PageRequest pageRequest = PageRequest.of(0, size);
        Slice<ChatMessage> messageSlice = chatMessageRepository.findByRoomIdWithCursor(roomId, lastMessageId, pageRequest);

        List<ChatDTO.ChatMessagePayload> content = messageSlice.getContent().stream()
                .map(ChatMapper::toPayload)
                .toList();

        Long nextCursor = content.isEmpty() ? null : content.get(content.size() - 1).getMessageId();

        log.info("채팅 메시지 조회 완료 - memberId={}, roomId={}, count={}, hasNext={}, nextCursor={}",
                memberId, roomId, content.size(), messageSlice.hasNext(), nextCursor);

        return CursorResponse.<ChatDTO.ChatMessagePayload>builder()
                .content(content)
                .nextCursor(nextCursor)
                .hasNext(messageSlice.hasNext())
                .build();
    }

    public ChatDTO.ChatRoomResponse getChatRoomResponse(Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findByIdWithSummary(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        return ChatMapper.toChatRoomResponse(chatRoom, null);
    }

    private List<Member> getParticipants(ChatRoom chatRoom) {
        log.debug("채팅방 참여자 조회 시작 - roomId={}, roomType={}", chatRoom.getId(), chatRoom.getRoomType());

        List<Member> participants;
        if (chatRoom.getRoomType() == ChatRoomType.ONE_ON_ONE || chatRoom.getRoomType() == ChatRoomType.ONE_ON_ONE_DIRECT) {
            Set<Member> directParticipants = new LinkedHashSet<>();
            if (chatRoom.getInitiator() != null) {
                directParticipants.add(chatRoom.getInitiator());
            }
            if (chatRoom.getReceiver() != null) {
                directParticipants.add(chatRoom.getReceiver());
            }
            participants = new ArrayList<>(directParticipants);
        } else {
            participants = chatRoom.getParticipants().stream()
                    .map(ChatRoomParticipant::getMember)
                    .distinct()
                    .toList();
        }

        log.debug("채팅방 참여자 조회 완료 - roomId={}, 참여자 수={}", chatRoom.getId(), participants.size());
        return participants;
    }

    @Transactional
    public Long createGroupChatRoom(Long memberId, Long postId, ChatRoomType chatRoomType) {
        log.info("채팅방 생성 시작 - memberId={}, postId={}, chatRoomType={}", memberId, postId, chatRoomType);

        Post post = getPostForGroupChatRoom(postId, chatRoomType);
        if (!post.getAuthor().getId().equals(memberId)) {
            log.warn("채팅방 생성 접근 거부 - memberId={}, postId={}", memberId, postId);
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        Long roomId = chatRoomRepository.findByPostId(postId)
                .map(ChatRoom::getId)
                .orElseGet(() -> {
                    ChatRoom newRoom = ChatRoomMapper.toEntity(chatRoomType, post, null, null);
                    addGroupChatParticipants(newRoom, post, chatRoomType);
                    Long newRoomId = chatRoomRepository.save(newRoom).getId();
                    log.info("새 그룹 채팅방 생성 완료 - roomId={}", newRoomId);
                    return newRoomId;
                });

        log.info("채팅방 생성 완료 - memberId={}, postId={}, roomId={}", memberId, postId, roomId);
        return roomId;
    }

    private Post getPostForGroupChatRoom(Long postId, ChatRoomType chatRoomType) {
        if (chatRoomType == ChatRoomType.GROUP_CREW) {
            return postRepository.findByIdWithParticipants(postId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        }

        return postRepository.findByIdWithAuthor(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    private void addGroupChatParticipants(ChatRoom chatRoom, Post post, ChatRoomType chatRoomType) {
        chatRoom.addParticipant(post.getAuthor());

        if (chatRoomType != ChatRoomType.GROUP_CREW) {
            return;
        }

        post.getParticipants().stream()
                .map(CrewParticipant::getMember)
                .forEach(chatRoom::addParticipant);
    }

    @Transactional
    public Long createOrFindDirectDm(Long senderId, Long targetUserId) {
        log.info("직접 DM 조회/생성 시작 - senderId={}, targetUserId={}", senderId, targetUserId);

        if (senderId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return chatRoomRepository.findDirectDmBetween(senderId, targetUserId, ChatRoomType.ONE_ON_ONE_DIRECT)
                .map(room -> {
                    log.info("기존 직접 DM 반환 - roomId={}", room.getId());
                    return room.getId();
                })
                .orElseGet(() -> {
                    Member initiator = memberRepository.findById(senderId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
                    Member receiver = memberRepository.findById(targetUserId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

                    ChatRoom newRoom = ChatRoomMapper.toEntity(ChatRoomType.ONE_ON_ONE_DIRECT, null, initiator, receiver);
                    Long newRoomId = chatRoomRepository.save(newRoom).getId();
                    log.info("새 직접 DM 생성 완료 - roomId={}", newRoomId);
                    return newRoomId;
                });
    }
}
