package com.Wolgyesangdan.backend.domain.campaign.repository;

import com.Wolgyesangdan.backend.domain.campaign.entity.Campaign;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
}
