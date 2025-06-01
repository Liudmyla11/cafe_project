package com.example.cafe;

import com.example.cafe.model.*;
import com.example.cafe.repository.*;
import com.example.cafe.controller.AdminController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AdminControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CafeRepository cafeRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @InjectMocks
    private AdminController adminController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllUsers_returnsListOfUsers() {
        List<User> users = List.of(new User(), new User());
        when(userRepository.findAll()).thenReturn(users);

        ResponseEntity<List<User>> response = adminController.getAllUsers();

        assertThat(response.getBody()).isEqualTo(users);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
    }

    @Test
    void getMenuItemsByCafe_cafeNotExists_returnsNotFound() {
        when(cafeRepository.existsById(1L)).thenReturn(false);

        ResponseEntity<List<MenuItem>> response = adminController.getMenuItemsByCafe(1L);

        assertThat(response.getStatusCodeValue()).isEqualTo(404);
    }

    @Test
    void getMenuItemsByCafe_cafeExists_returnsMenuItems() {
        when(cafeRepository.existsById(1L)).thenReturn(true);
        List<MenuItem> items = List.of(new MenuItem(), new MenuItem());
        when(menuItemRepository.findByCafeId(1L)).thenReturn(items);

        ResponseEntity<List<MenuItem>> response = adminController.getMenuItemsByCafe(1L);

        assertThat(response.getBody()).isEqualTo(items);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
    }

    @Test
    void createUser_usernameExists_returnsBadRequest() {
        User user = new User();
        user.setUsername("existingUser");

        when(userRepository.findByUsername("existingUser")).thenReturn(Optional.of(new User()));

        ResponseEntity<?> response = adminController.createUser(user);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("Ім'я користувача вже існує");
    }

    @Test
    void updateUser_userNotFound_returnsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = adminController.updateUser(1L, new User());

        assertThat(response.getStatusCodeValue()).isEqualTo(404);
    }

    @Test
    void updateUser_usernameConflict_returnsConflict() {
        User existingUser = new User();
        existingUser.setId(2L);
        existingUser.setUsername("user2");

        User updatedUser = new User();
        updatedUser.setUsername("user2");

        User userToUpdate = new User();
        userToUpdate.setId(1L);
        userToUpdate.setUsername("user1");

        when(userRepository.findById(1L)).thenReturn(Optional.of(userToUpdate));
        when(userRepository.findByUsername("user2")).thenReturn(Optional.of(existingUser));

        ResponseEntity<?> response = adminController.updateUser(1L, updatedUser);

        assertThat(response.getStatusCodeValue()).isEqualTo(409);
        assertThat(response.getBody()).isEqualTo("Користувач з таким іменем вже існує");
    }

    @Test
    void deleteUser_userNotFound_returnsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = adminController.deleteUser(1L);

        assertThat(response.getStatusCodeValue()).isEqualTo(404);
    }

    @Test
    void deleteUser_userIsManagerWithCafe_returnsBadRequest() {
        User manager = new User();
        manager.setRoles(Set.of("ROLE_MANAGER"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(cafeRepository.existsByManager(manager)).thenReturn(true);

        ResponseEntity<?> response = adminController.deleteUser(1L);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("Менеджера видалити неможливо, адже за ним закріплене кафе");
    }

    @Test
    void deleteUser_validUser_deletesAndReturnsOk() {
        User user = new User();
        user.setRoles(Set.of("ROLE_CUSTOMER"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cafeRepository.existsByManager(user)).thenReturn(false);
        doNothing().when(userRepository).deleteById(1L);

        ResponseEntity<?> response = adminController.deleteUser(1L);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void createCafe_missingName_returnsBadRequest() {
        Cafe cafe = new Cafe();
        cafe.setName("  "); // empty name

        ResponseEntity<?> response = adminController.createCafe(cafe);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("Назва кафе є обов'язковою");
    }

    @Test
    void createCafe_managerNotFound_returnsBadRequest() {
        Cafe cafe = new Cafe();
        cafe.setName("Cafe");
        cafe.setCity("City");

        User manager = new User();
        manager.setId(10L);
        cafe.setManager(manager);

        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = adminController.createCafe(cafe);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("Менеджера не знайдено");
    }

    @Test
    void createCafe_validCafe_returnsSaved() {
        Cafe cafe = new Cafe();
        cafe.setName("Cafe");
        cafe.setCity("City");

        User manager = new User();
        manager.setId(10L);
        manager.setRoles(Set.of("ROLE_MANAGER"));
        cafe.setManager(manager);

        when(userRepository.findById(10L)).thenReturn(Optional.of(manager));
        when(cafeRepository.save(cafe)).thenReturn(cafe);

        ResponseEntity<?> response = adminController.createCafe(cafe);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(cafe);
    }

    @Test
    void createMenuItem_missingName_returnsBadRequest() {
        MenuItem item = new MenuItem();
        item.setName(" ");
        item.setCategory("Category");
        item.setPrice(10.0);
        item.setCafe(new Cafe());
        item.getCafe().setId(1L);

        ResponseEntity<?> response = adminController.createMenuItem(item);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("Назва є обов’язковою");
    }

    @Test
    void createMenuItem_cafeNotFound_returnsBadRequest() {
        MenuItem item = new MenuItem();
        item.setName("Name");
        item.setCategory("Category");
        item.setPrice(10.0);
        Cafe cafe = new Cafe();
        cafe.setId(1L);
        item.setCafe(cafe);

        when(cafeRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = adminController.createMenuItem(item);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("Кафе не знайдено");
    }

    @Test
    void createMenuItem_validItem_returnsSaved() {
        MenuItem item = new MenuItem();
        item.setName("Name");
        item.setCategory("Category");
        item.setPrice(10.0);
        Cafe cafe = new Cafe();
        cafe.setId(1L);
        item.setCafe(cafe);

        when(cafeRepository.findById(1L)).thenReturn(Optional.of(cafe));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<?> response = adminController.createMenuItem(item);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        MenuItem saved = (MenuItem) response.getBody();
        assertThat(saved.getCafe()).isEqualTo(cafe);
    }
}
