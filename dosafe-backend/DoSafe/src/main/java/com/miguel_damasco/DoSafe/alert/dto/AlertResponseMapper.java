package com.miguel_damasco.DoSafe.alert.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.miguel_damasco.DoSafe.alert.domain.AlertModel;

public class AlertResponseMapper {

    public static AlertResponseDTO toDto(AlertModel pAlert) {
        String documentType = pAlert.getDocument().getType() != null
                ? pAlert.getDocument().getType().name()
                : null;

        return new AlertResponseDTO(
                pAlert.getId(),
                pAlert.getDocument().getId().toString(),
                documentType,
                pAlert.getDocument().getOriginalFilename(),
                pAlert.getDocument().getExpireAt(),
                pAlert.getSentAt(),
                pAlert.isRead());
    }

    public static AlertPageResponseDTO toPageDto(Page<AlertModel> pPage) {
        List<AlertResponseDTO> content = pPage.getContent()
                .stream()
                .map(AlertResponseMapper::toDto)
                .toList();

        return new AlertPageResponseDTO(
                content,
                pPage.getNumber(),
                pPage.getSize(),
                pPage.getTotalElements(),
                pPage.getTotalPages(),
                pPage.isLast());
    }

    private AlertResponseMapper() {}
}
