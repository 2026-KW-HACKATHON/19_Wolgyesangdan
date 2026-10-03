package com.Wolgyesangdan.backend.domain.user.dto;

import com.Wolgyesangdan.backend.domain.user.entity.ContactType;
import com.Wolgyesangdan.backend.domain.user.entity.User;

public record ContactResponse(ContactType contactType, String phone, String openchatLink) {

	public static ContactResponse from(User user) {
		return new ContactResponse(user.getContactType(), user.getPhone(), user.getOpenchatLink());
	}

}
