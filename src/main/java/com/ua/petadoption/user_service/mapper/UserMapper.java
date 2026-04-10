package com.ua.petadoption.user_service.mapper;

import com.ua.petadoption.commons.user.UserDTO;
import com.ua.petadoption.user_service.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDto(User user);
}
