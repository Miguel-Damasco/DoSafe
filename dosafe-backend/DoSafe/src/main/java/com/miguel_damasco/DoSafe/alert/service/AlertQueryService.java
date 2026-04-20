package com.miguel_damasco.DoSafe.alert.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miguel_damasco.DoSafe.alert.domain.AlertModel;
import com.miguel_damasco.DoSafe.alert.dto.AlertPageResponseDTO;
import com.miguel_damasco.DoSafe.alert.dto.AlertResponseMapper;
import com.miguel_damasco.DoSafe.alert.repository.AlertRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class AlertQueryService {

    private final AlertRepository alertRepository;

    public long countUnread(String pUsername) {
        long count = alertRepository.countByUserUsernameAndSentAtIsNotNullAndReadAtIsNull(pUsername);
        log.debug("Unread alert count username={} count={}", pUsername, count);
        return count;
    }

    public AlertPageResponseDTO listSentAlerts(String pUsername, int pPage, int pSize) {
        Pageable pageable = PageRequest.of(pPage, Math.min(pSize, 50));
        Page<AlertModel> page = alertRepository.findSentAlertsByUsername(pUsername, pageable);
        log.info("List alerts username={} page={} total={}", pUsername, pPage, page.getTotalElements());
        return AlertResponseMapper.toPageDto(page);
    }

    @Transactional
    public void markAllRead(String pUsername) {
        int updated = alertRepository.markAllReadByUsername(pUsername, Instant.now());
        log.info("Marked alerts as read username={} count={}", pUsername, updated);
    }
}
