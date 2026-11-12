package com.nueetemoins.backend.family.dto;

import java.util.List;

public record FamilyAdminResponse(String publicId, List<FamilyTranslationRequest> translations) {}
