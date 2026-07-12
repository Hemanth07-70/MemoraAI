package com.memoraai.profile.mapper;

import com.memoraai.profile.dto.ProfileResponse;
import com.memoraai.profile.entity.UserProfile;
import com.memoraai.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {UserMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserProfileMapper {
    ProfileResponse toResponse(UserProfile profile);
}
