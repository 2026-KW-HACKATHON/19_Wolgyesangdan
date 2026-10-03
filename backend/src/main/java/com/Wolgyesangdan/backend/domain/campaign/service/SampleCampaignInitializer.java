package com.Wolgyesangdan.backend.domain.campaign.service;

import java.time.LocalDate;

import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;
import com.Wolgyesangdan.backend.domain.campaign.entity.CampaignStatus;
import com.Wolgyesangdan.backend.domain.campaign.repository.CampaignRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ⚠️ 로컬 개발 전용. campaigns 테이블이 비어 있으면 진행 중(ACTIVE)인 샘플 캠페인을 하나 만든다.
 * 실제 캠페인은 운영진이 DB에 직접 입력하므로 local 프로필에서만 등록된다.
 *
 * 날짜는 서버 기동일 기준 상대값이라 생성 시점엔 항상 진행 중이다. 시간이 지나 끝난 캠페인이 되면
 * 해당 행을 지우고 재기동하면 오늘 기준으로 다시 만들어진다. 이미 캠페인이 있으면 아무것도 안 한다.
 */
@Slf4j
@Profile("local")
@Component
@RequiredArgsConstructor
public class SampleCampaignInitializer implements ApplicationRunner {

	static final String SAMPLE_NAME = "자원순환 캠페인 (로컬 샘플)";

	private final CampaignRepository campaignRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (campaignRepository.count() > 0) {
			return;
		}
		Campaign campaign = campaignRepository.save(createSample(LocalDate.now()));
		log.info("로컬 샘플 캠페인 생성: id={}, 신청 기간 {} ~ {}", campaign.getId(),
				campaign.getApplicationStartDate(), campaign.getApplicationEndDate());
	}

	static Campaign createSample(LocalDate today) {
		return Campaign.builder()
				.name(SAMPLE_NAME)
				.description("이사철에 버려질 뻔한 물건을 월계1동 안에서 다시 쓰도록 잇는 캠페인이에요.")
				.registrationStartDate(today.minusDays(7))
				.registrationEndDate(today.plusDays(30))
				.applicationStartDate(today.minusDays(3))
				.applicationEndDate(today.plusDays(30))
				.pickupStartDate(today.minusDays(3))
				.pickupEndDate(today.plusDays(32))
				.locationName("광운대 비마관 1층 거점")
				.locationAddress("서울 노원구 광운로 20")
				.hubHours("평일 10:00 ~ 18:00")
				.status(CampaignStatus.ACTIVE)
				.build();
	}

}
