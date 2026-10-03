package com.Wolgyesangdan.backend.domain.item.repository;

import java.util.Optional;

import com.Wolgyesangdan.backend.domain.item.entity.CategoryCarbonReference;
import com.Wolgyesangdan.backend.domain.item.entity.CategoryGroup;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryCarbonReferenceRepository extends JpaRepository<CategoryCarbonReference, Long> {

	Optional<CategoryCarbonReference> findByCategoryGroup(CategoryGroup categoryGroup);

}
