package controller;
import ru.maxon.controller.UserController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.maxon.dto.UserResponse;
import ru.maxon.entity.User;
import ru.maxon.exception.EmailAlreadyExistsException;
import ru.maxon.exception.GlobalExceptionHandler;
import ru.maxon.exception.UserNotFoundException;
import ru.maxon.mapper.UserMapper;
import ru.maxon.service.UserService;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@ContextConfiguration(classes = {
        UserController.class,
        GlobalExceptionHandler.class
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    private User user;
    private UserResponse response;

    @BeforeEach
    void setUp() {
        user = new User("Maxim", "maxim@example.com", 23);

        response = new UserResponse(
                1L,
                "Maxim",
                "maxim@example.com",
                23,
                Instant.parse("2026-10-15T18:00:00Z")
        );
    }

    @Test
    void findAllShouldReturnUsers() throws Exception {
        when(userService.findAll()).thenReturn(List.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Maxim"))
                .andExpect(jsonPath("$[0].email")
                        .value("maxim@example.com"))
                .andExpect(jsonPath("$[0].age").value(23));

        verify(userService).findAll();
    }

    @Test
    void findAllShouldReturnEmptyList() throws Exception {
        when(userService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(userService).findAll();
        verifyNoInteractions(userMapper);
    }

    @Test
    void findByIdShouldReturnUser() throws Exception {
        when(userService.findById(1L)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Maxim"))
                .andExpect(jsonPath("$.email")
                        .value("maxim@example.com"))
                .andExpect(jsonPath("$.age").value(23))
                .andExpect(jsonPath("$.createdAt")
                        .value("2026-10-15T18:00:00Z"));

        verify(userService).findById(1L);
    }

    @Test
    void createShouldReturnCreatedUserAndLocation() throws Exception {
        when(userService.create("Maxim", "maxim@example.com", 23))
                .thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Maxim",
                                  "email": "maxim@example.com",
                                  "age": 23
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/users/1"
                ))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Maxim"))
                .andExpect(jsonPath("$.email")
                        .value("maxim@example.com"))
                .andExpect(jsonPath("$.age").value(23));

        verify(userService).create(
                "Maxim",
                "maxim@example.com",
                23
        );
    }

    @Test
    void updateShouldReturnUpdatedUser() throws Exception {
        UserResponse updatedResponse = new UserResponse(
                1L,
                "Maxim Updated",
                "maxim.updated@example.com",
                24,
                response.createdAt()
        );

        when(userService.update(
                1L,
                "Maxim Updated",
                "maxim.updated@example.com",
                24
        )).thenReturn(user);

        when(userMapper.toResponse(user)).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Maxim Updated",
                                  "email": "maxim.updated@example.com",
                                  "age": 24
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Maxim Updated"))
                .andExpect(jsonPath("$.email")
                        .value("maxim.updated@example.com"))
                .andExpect(jsonPath("$.age").value(24));

        verify(userService).update(
                1L,
                "Maxim Updated",
                "maxim.updated@example.com",
                24
        );
    }

    @Test
    void deleteShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(userService).delete(1L);
        verifyNoInteractions(userMapper);
    }

    @Test
    void findByIdShouldReturnNotFound() throws Exception {
        when(userService.findById(999L))
                .thenThrow(UserNotFoundException.class);

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.instance")
                        .value("/api/users/999"));

        verify(userService).findById(999L);
        verifyNoInteractions(userMapper);
    }

    @Test
    void createShouldReturnConflictForDuplicateEmail() throws Exception {
        when(userService.create("Maxim", "maxim@example.com", 23))
                .thenThrow(EmailAlreadyExistsException.class);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Maxim",
                                  "email": "maxim@example.com",
                                  "age": 23
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409));

        verify(userService).create(
                "Maxim",
                "maxim@example.com",
                23
        );
        verifyNoInteractions(userMapper);
    }

    @Test
    void findByIdShouldRejectNonNumericId() throws Exception {
        mockMvc.perform(get("/api/users/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(
                        "Некорректное значение параметра: id"
                ));

        verifyNoInteractions(userService, userMapper);
    }

    @Test
    void createShouldRejectMalformedJson() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(
                        "Тело запроса отсутствует или содержит некорректный JSON"
                ));

        verifyNoInteractions(userService, userMapper);
    }

    @Test
    void createShouldRejectBlankName() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   ",
                                  "email": "maxim@example.com",
                                  "age": 23
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(
                        "Проверьте данные запроса"
                ))
                .andExpect(jsonPath("$.errors.name").value(
                        "Имя не должно быть пустым"
                ));

        verifyNoInteractions(userService, userMapper);
    }

    @Test
    void createShouldRejectTooLongName() throws Exception {
        String request = """
                {
                  "name": "%s",
                  "email": "maxim@example.com",
                  "age": 23
                }
                """.formatted("a".repeat(101));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value(
                        "Имя не должно быть длиннее 100 символов"
                ));

        verifyNoInteractions(userService, userMapper);
    }

    @Test
    void createShouldRejectBlankEmail() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Maxim",
                                  "email": "",
                                  "age": 23
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value(
                        "Email не должен быть пустым"
                ));

        verifyNoInteractions(userService, userMapper);
    }

    @Test
    void createShouldRejectInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Maxim",
                                  "email": "invalid-email",
                                  "age": 23
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value(
                        "Укажите корректный email"
                ));

        verifyNoInteractions(userService, userMapper);
    }

    @Test
    void createShouldRejectMissingAge() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Maxim",
                                  "email": "maxim@example.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.age").value(
                        "Возраст обязателен"
                ));

        verifyNoInteractions(userService, userMapper);
    }

    @Test
    void createShouldRejectNegativeAge() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Maxim",
                                  "email": "maxim@example.com",
                                  "age": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.age").value(
                        "Возраст не может быть отрицательным"
                ));

        verifyNoInteractions(userService, userMapper);
    }

    @Test
    void createShouldRejectAgeAboveMaximum() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Maxim",
                                  "email": "maxim@example.com",
                                  "age": 151
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.age").value(
                        "Возраст не должен превышать 150"
                ));

        verifyNoInteractions(userService, userMapper);
    }

    @Test
    void createShouldReturnAllInvalidFields() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "invalid-email",
                                  "age": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value(
                        "Имя не должно быть пустым"
                ))
                .andExpect(jsonPath("$.errors.email").value(
                        "Укажите корректный email"
                ))
                .andExpect(jsonPath("$.errors.age").value(
                        "Возраст не может быть отрицательным"
                ));

        verifyNoInteractions(userService, userMapper);
    }

    @Test
    void updateShouldRejectInvalidRequest() throws Exception {
        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "invalid-email",
                                  "age": 151
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value(
                        "Имя не должно быть пустым"
                ))
                .andExpect(jsonPath("$.errors.email").value(
                        "Укажите корректный email"
                ))
                .andExpect(jsonPath("$.errors.age").value(
                        "Возраст не должен превышать 150"
                ));

        verifyNoInteractions(userService, userMapper);
    }
}