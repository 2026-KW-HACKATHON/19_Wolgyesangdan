package com.Wolgyesangdan.backend.domain.inquiry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryAnswerRequest(@NotBlank @Size(max = 2000) String answer) {

}
