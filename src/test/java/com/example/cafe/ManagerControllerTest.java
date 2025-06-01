package com.example.cafe;

import com.example.cafe.model.*;
import com.example.cafe.repository.*;
import com.example.cafe.controller.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ManagerControllerTest {

    @Mock
    private CafeRepository cafeRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ManagerController managerController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getManagerCafes_managerNotFound_returnsForbidden() {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("manager1");
        when(userRepository.findByUsername("manager1")).thenReturn(Optional.empty());

        ResponseEntity<?> response = managerController.getManagerCafes(userDetails);

        assertThat(response.getStatusCodeValue()).isEqualTo(403);
        assertThat(response.getBody()).isEqualTo("Менеджера не знайдено");
    }

    @Test
    void getManagerCafes_managerFound_returnsCafes() {
        User manager = new User();
        List<Cafe> cafes = List.of(new Cafe(), new Cafe());

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("manager1");
        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
        when(cafeRepository.findByManager(manager)).thenReturn(cafes);

        ResponseEntity<?> response = managerController.getManagerCafes(userDetails);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(cafes);
    }

    @Test
    void getCafeMenu_managerNotFound_returnsForbidden() {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("manager1");
        when(userRepository.findByUsername("manager1")).thenReturn(Optional.empty());

        ResponseEntity<?> response = managerController.getCafeMenu(1L, userDetails);

        assertThat(response.getStatusCodeValue()).isEqualTo(403);
        assertThat(response.getBody()).isEqualTo("Менеджера не знайдено");
    }

    @Test
    void getCafeMenu_cafeOwned_returnsMenu() {
        User manager = new User();
        Cafe cafe = new Cafe();
        cafe.setManager(manager);

        List<MenuItem> menu = List.of(new MenuItem(), new MenuItem());

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("manager1");
        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
        when(cafeRepository.findById(1L)).thenReturn(Optional.of(cafe));
        when(menuItemRepository.findByCafeId(1L)).thenReturn(menu);

        ResponseEntity<?> response = managerController.getCafeMenu(1L, userDetails);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(menu);
    }

    @Test
    void getOrdersFromManagedCafes_managerNotFound_returnsForbidden() {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("manager1");
        when(userRepository.findByUsername("manager1")).thenReturn(Optional.empty());

        ResponseEntity<?> response = managerController.getOrdersFromManagedCafes(userDetails, null);

        assertThat(response.getStatusCodeValue()).isEqualTo(403);
        assertThat(response.getBody()).isEqualTo("Менеджера не знайдено");
    }

    @Test
    void getOrdersFromManagedCafes_cafeIdNotOwned_returnsForbidden() {
        User manager = new User();
        Cafe managedCafe = new Cafe();
        managedCafe.setId(2L);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("manager1");
        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
        when(cafeRepository.findByManager(manager)).thenReturn(List.of(managedCafe));

        ResponseEntity<?> response = managerController.getOrdersFromManagedCafes(userDetails, 1L);

        assertThat(response.getStatusCodeValue()).isEqualTo(403);
        assertThat(response.getBody()).isEqualTo("Ця кав'ярня вам не належить");
    }

    @Test
    void getOrdersFromManagedCafes_validCafe_returnsOrders() {
        User manager = new User();
        Cafe cafe1 = new Cafe();
        cafe1.setId(1L);
        Cafe cafe2 = new Cafe();
        cafe2.setId(2L);

        Order order1 = new Order();
        order1.setCafe(cafe1);
        Order order2 = new Order();
        order2.setCafe(cafe2);
        Order order3 = new Order();
        order3.setCafe(null); // no cafe

        List<Order> allOrders = List.of(order1, order2, order3);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("manager1");
        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
        when(cafeRepository.findByManager(manager)).thenReturn(List.of(cafe1, cafe2));
        when(orderRepository.findAll()).thenReturn(allOrders);

        ResponseEntity<?> response = managerController.getOrdersFromManagedCafes(userDetails, null);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        List<Order> resultOrders = (List<Order>) response.getBody();
        assertThat(resultOrders).containsExactlyInAnyOrder(order1, order2);
    }

    @Test
    void createMenuItem_invalidName_returnsBadRequest() {
        MenuItem item = new MenuItem();
        item.setName(" ");
        item.setCategory("category");
        item.setPrice(10.0);
        item.setCafe(new Cafe());
        item.getCafe().setId(1L);

        ResponseEntity<?> response = managerController.createMenuItem(item);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("Назва є обов’язковою");
    }

    @Test
    void createMenuItem_invalidCategory_returnsBadRequest() {
        MenuItem item = new MenuItem();
        item.setName("Name");
        item.setCategory(" ");
        item.setPrice(10.0);
        item.setCafe(new Cafe());
        item.getCafe().setId(1L);

        ResponseEntity<?> response = managerController.createMenuItem(item);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("Категорія є обов’язковою");
    }

    @Test
    void createMenuItem_invalidPrice_returnsBadRequest() {
        MenuItem item = new MenuItem();
        item.setName("Name");
        item.setCategory("Category");
        item.setPrice(-5.0);
        item.setCafe(new Cafe());
        item.getCafe().setId(1L);

        ResponseEntity<?> response = managerController.createMenuItem(item);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("Ціна має бути додатною та більшою за 0");
    }

    @Test
    void createMenuItem_cafeNull_returnsBadRequest() {
        MenuItem item = new MenuItem();
        item.setName("Name");
        item.setCategory("Category");
        item.setPrice(10.0);
        item.setCafe(null);

        ResponseEntity<?> response = managerController.createMenuItem(item);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("Кафе є обов’язковим");
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

        ResponseEntity<?> response = managerController.createMenuItem(item);

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

        ResponseEntity<?> response = managerController.createMenuItem(item);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        MenuItem saved = (MenuItem) response.getBody();
        assertThat(saved.getCafe()).isEqualTo(cafe);
    }
}

