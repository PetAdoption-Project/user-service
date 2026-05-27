package com.ua.petadoption.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.petadoption.commons.exception.GlobalExceptionHandler;
import com.ua.petadoption.user_service.service.AuthService;
import com.ua.petadoption.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import(GlobalExceptionHandler.class)
abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected AuthService authService;

    @MockitoBean
    protected UserService userService;
}
