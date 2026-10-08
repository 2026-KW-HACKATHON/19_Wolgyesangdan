package com.Wolgyesangdan.backend.domain.item.entity;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 품목 — 카테고리 대분류 아래의 세부 품목과 품목별 예상 탄소 절감량 (#284).
 * 대분류 하나로만 계산하면 냉장고와 선풍기가 같은 값이 나와서, 자주 나오는 품목은 따로 값을 둔다.
 * 목록에 없는 물건은 품목 없이 등록하고 대분류 값(CategoryCarbonReference)을 쓴다.
 *
 * 값은 1:1 대체 가정(중고 1건이 새 제품 1개 생산을 대체)으로 계산했다.
 * - 품목 연구값이 있으면 그 값 (WRAP 2012 가구 재사용 연구: 소파·식탁)
 * - 없으면 대표 무게 × 재질별 생산 배출계수 (UK DESNZ 2024 Material use, Primary material production)
 * 자세한 계산은 옵시디언 「품목별 탄소 절감량 참조표」 참고. 대분류 안의 순서가 등록 화면 표시 순서다.
 */
@Getter
@RequiredArgsConstructor
public enum ItemType {

	// 가구
	SOFA(CategoryGroup.FURNITURE, "소파", 55, "소파 1개 · WRAP 2012 가구 재사용 연구"),
	DINING_TABLE(CategoryGroup.FURNITURE, "식탁", 20, "식탁 1개 · WRAP 2012 가구 재사용 연구"),
	CHAIR(CategoryGroup.FURNITURE, "의자", 39, "사무용 의자 12kg(금속·플라스틱) · UK DESNZ 2024"),
	WARDROBE(CategoryGroup.FURNITURE, "옷장", 32, "2도어 옷장 60kg(목재·금속) · UK DESNZ 2024"),
	DESK(CategoryGroup.FURNITURE, "책상", 23, "1인용 책상 25kg(목재·금속) · UK DESNZ 2024"),
	BED_FRAME(CategoryGroup.FURNITURE, "침대 프레임", 18, "싱글 프레임 33kg(목재·금속) · UK DESNZ 2024"),
	DRAWER(CategoryGroup.FURNITURE, "서랍장", 14, "4단 서랍장 30kg(목재·금속) · UK DESNZ 2024"),
	BOOKSHELF(CategoryGroup.FURNITURE, "책장", 10, "5단 책장 25kg(목재·금속) · UK DESNZ 2024"),

	// 가전
	REFRIGERATOR(CategoryGroup.APPLIANCE, "냉장고", 240, "2도어 300L급 55kg · UK DESNZ 2024 냉장·냉동 기기"),
	TV(CategoryGroup.APPLIANCE, "TV", 199, "32~43인치 8kg · UK DESNZ 2024 IT 기기"),
	WASHING_MACHINE(CategoryGroup.APPLIANCE, "세탁기", 124, "통돌이 10kg급 38kg · UK DESNZ 2024 대형 가전"),
	MINI_FRIDGE(CategoryGroup.APPLIANCE, "소형 냉장고", 109, "1인용 90L급 25kg · UK DESNZ 2024 냉장·냉동 기기"),
	MONITOR(CategoryGroup.APPLIANCE, "모니터", 99, "24인치 4kg · UK DESNZ 2024 IT 기기"),
	MICROWAVE(CategoryGroup.APPLIANCE, "전자레인지", 68, "20L급 12kg · UK DESNZ 2024 소형 가전"),
	AIR_FRYER(CategoryGroup.APPLIANCE, "에어프라이어", 28, "5L급 5kg · UK DESNZ 2024 소형 가전"),
	FAN(CategoryGroup.APPLIANCE, "선풍기", 28, "스탠드형 5kg · UK DESNZ 2024 소형 가전"),
	RICE_COOKER(CategoryGroup.APPLIANCE, "전기밥솥", 23, "6인용 4kg · UK DESNZ 2024 소형 가전"),
	VACUUM(CategoryGroup.APPLIANCE, "청소기", 23, "유선 4kg · UK DESNZ 2024 소형 가전"),
	KETTLE(CategoryGroup.APPLIANCE, "전기포트", 7, "1.7L급 1.2kg · UK DESNZ 2024 소형 가전"),

	// 주방
	COOKWARE(CategoryGroup.KITCHEN, "냄비·프라이팬", 7, "2kg · UK DESNZ 2024 금속"),
	TABLEWARE(CategoryGroup.KITCHEN, "그릇·식기", 4, "세트 3kg · UK DESNZ 2024 유리(도자기 근사)"),
	UTENSILS(CategoryGroup.KITCHEN, "수저·조리도구", 3, "1kg · UK DESNZ 2024 금속"),

	// 생활
	HANGER(CategoryGroup.LIVING, "행거", 17, "스탠드 행거 5kg · UK DESNZ 2024 금속"),
	STORAGE_BOX(CategoryGroup.LIVING, "플라스틱 수납함", 16, "3단 5kg · UK DESNZ 2024 플라스틱"),
	DESK_LAMP(CategoryGroup.LIVING, "스탠드 조명", 11, "2kg · UK DESNZ 2024 소형 가전"),
	DRYING_RACK(CategoryGroup.LIVING, "빨래 건조대", 10, "3kg · UK DESNZ 2024 금속"),
	MIRROR(CategoryGroup.LIVING, "전신 거울", 7, "6kg(유리·목재) · UK DESNZ 2024"),

	// 기타
	BICYCLE(CategoryGroup.ETC, "자전거", 45, "일반 자전거 13kg · UK DESNZ 2024 금속");

	private final CategoryGroup categoryGroup;
	private final String label;
	/** 예상 탄소 절감량 (kg CO2e) */
	private final int carbonReductionKg;
	/** 화면에 보여줄 계산 근거 */
	private final String basis;

	/** 대분류에 속한 품목 (등록 화면 표시 순서) */
	public static List<ItemType> of(CategoryGroup categoryGroup) {
		return Arrays.stream(values()).filter(type -> type.categoryGroup == categoryGroup).toList();
	}

}
