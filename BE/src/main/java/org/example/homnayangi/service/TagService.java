package org.example.homnayangi.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.homnayangi.dto.request.TagRequest;
import org.example.homnayangi.dto.response.TagResponse;
import org.example.homnayangi.dto.response.UserTagResponse;
import org.example.homnayangi.entity.Tag;
import org.example.homnayangi.entity.User;
import org.example.homnayangi.entity.UserTag;
import org.example.homnayangi.entity.UserTagId;
import org.example.homnayangi.exception.AppException;
import org.example.homnayangi.exception.ErrorCode;
import org.example.homnayangi.mapper.TagMapper;
import org.example.homnayangi.repository.TagRepository;
import org.example.homnayangi.repository.UserRepository;
import org.example.homnayangi.repository.UserTagRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class TagService {

    TagRepository tagRepository;
    UserTagRepository userTagRepository;
    UserRepository userRepository;
    TagMapper tagMapper;

    public TagResponse createTag(TagRequest request) {
        if (tagRepository.existsByTagName(request.getTagName())) {
            throw new AppException(ErrorCode.TAG_ALREADY_EXISTS);
        }

        Tag tag = tagMapper.toTag(request);
        return tagMapper.toTagResponse(tagRepository.save(tag));
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        return tagRepository.findAll().stream()
                .map(tagMapper::toTagResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TagResponse getTag(UUID id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));
        return tagMapper.toTagResponse(tag);
    }

    public void deleteTag(UUID id) {
        if (!tagRepository.existsById(id)) {
            throw new AppException(ErrorCode.TAG_NOT_FOUND);
        }
        tagRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<UserTagResponse> getUserTags() {
        User currentUser = getCurrentUser();
        List<UserTag> userTags = userTagRepository.findAllByUserIdWithTag(currentUser.getId());
        return userTags.stream()
                .map(tagMapper::toUserTagResponse)
                .toList();
    }

    public UserTagResponse addUserTag(UUID tagId, boolean temporary) {
        User currentUser = getCurrentUser();
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));

        UserTagId userTagId = new UserTagId(currentUser.getId(), tagId);
        UserTag userTag = userTagRepository.findById(userTagId)
                .orElse(null);

        if (userTag != null) {
            userTag.setTemporary(temporary);
        } else {
            userTag = UserTag.builder()
                    .id(userTagId)
                    .user(currentUser)
                    .tag(tag)
                    .temporary(temporary)
                    .build();
        }

        return tagMapper.toUserTagResponse(userTagRepository.save(userTag));
    }

    public void removeUserTag(UUID tagId) {
        User currentUser = getCurrentUser();
        UserTagId userTagId = new UserTagId(currentUser.getId(), tagId);

        if (!userTagRepository.existsById(userTagId)) {
            throw new AppException(ErrorCode.TAG_NOT_FOUND);
        }

        userTagRepository.deleteById(userTagId);
    }

    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }
}
