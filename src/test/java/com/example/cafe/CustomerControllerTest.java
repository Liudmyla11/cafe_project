package com.example.cafe;

import com.example.cafe.model.*;
import com.example.cafe.repository.*;
import com.example.cafe.controller.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class CustomerControllerTest {

    @Mock
    private CafeRepository cafeRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomerController customerController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllCafes_returnsAllCafes() {
        List<Cafe> cafes = List.of(new Cafe(), new Cafe());
        when(cafeRepository.findAll()).thenReturn(cafes);

        ResponseEntity<List<Cafe>> response = customerController.getAllCafes();

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(cafes);
    }

    @Test
    void getMenuByCafe_cafeNotExists_returnsNotFound() {
        when(cafeRepository.existsById(1L)).thenReturn(false);

        ResponseEntity<List<MenuItem>> response = customerController.getMenuByCafe(1L);

        assertThat(response.getStatusCodeValue()).isEqualTo(404);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void getMenuByCafe_cafeExists_returnsMenu() {
        when(cafeRepository.existsById(1L)).thenReturn(true);
        List<MenuItem> menu = List.of(new MenuItem(), new MenuItem());
        when(menuItemRepository.findByCafeId(1L)).thenReturn(menu);

        ResponseEntity<List<MenuItem>> response = customerController.getMenuByCafe(1L);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(menu);
    }

    @Test
    void createOrder_cafeNull_returnsBadRequest() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user1");
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(new User()));

        Order order = new Order();
        order.setCafe(null);

        ResponseEntity<?> response = customerController.createOrder(order, auth);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("Кафе є обов’язковим");
    }

    @Test
    void createOrder_cafeNotFound_returnsBadRequest() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user1");
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(new User()));

        Cafe cafe = new Cafe();
        cafe.setId(1L);

        Order order = new Order();
        order.setCafe(cafe);

        when(cafeRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = customerController.createOrder(order, auth);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("Кафе не знайдено");
    }

    @Test
    void createOrder_emptyItems_returnsBadRequest() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user1");
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(new User()));

        Cafe cafe = new Cafe();
        cafe.setId(1L);

        Order order = new Order();
        order.setCafe(cafe);
        order.setItems(Collections.emptyList());

        when(cafeRepository.findById(1L)).thenReturn(Optional.of(cafe));

        ResponseEntity<?> response = customerController.createOrder(order, auth);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("Список позицій замовлення не може бути порожнім");
    }

    @Test
    void createOrder_menuItemNotFound_returnsBadRequest() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user1");
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(new User()));

        Cafe cafe = new Cafe();
        cafe.setId(1L);

        MenuItem item = new MenuItem();
        item.setId(100L);

        Order order = new Order();
        order.setCafe(cafe);
        order.setItems(List.of(item));

        when(cafeRepository.findById(1L)).thenReturn(Optional.of(cafe));
        when(menuItemRepository.findById(100L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = customerController.createOrder(order, auth);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("Позицію меню не знайдено: ID = 100");
    }

    @Test
    void createOrder_menuItemNotBelongToCafe_returnsBadRequest() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user1");
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(new User()));

        Cafe cafe = new Cafe();
        cafe.setId(1L);

        Cafe otherCafe = new Cafe();
        otherCafe.setId(2L);

        MenuItem item = new MenuItem();
        item.setId(100L);
        item.setCafe(otherCafe);

        Order order = new Order();
        order.setCafe(cafe);
        order.setItems(List.of(item));

        when(cafeRepository.findById(1L)).thenReturn(Optional.of(cafe));
        when(menuItemRepository.findById(100L)).thenReturn(Optional.of(item));

        ResponseEntity<?> response = customerController.createOrder(order, auth);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("Позиція не належить до вибраного кафе");
    }

    @Test
    void createOrder_validOrder_returnsSavedOrder() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user1");

        User user = new User();
        user.setId(10L);

        Cafe cafe = new Cafe();
        cafe.setId(1L);

        MenuItem item = new MenuItem();
        item.setId(100L);
        item.setPrice(5.5);
        item.setCafe(cafe);

        Order orderRequest = new Order();
        orderRequest.setCafe(cafe);
        orderRequest.setItems(List.of(item));

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(cafeRepository.findById(1L)).thenReturn(Optional.of(cafe));
        when(menuItemRepository.findById(100L)).thenReturn(Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<?> response = customerController.createOrder(orderRequest, auth);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        Order savedOrder = (Order) response.getBody();
        assertThat(savedOrder.getUser()).isEqualTo(user);
        assertThat(savedOrder.getCafe()).isEqualTo(cafe);
        assertThat(savedOrder.getItems()).containsExactly(item);
        assertThat(savedOrder.getTotalAmount()).isEqualTo(5.5);
    }

    @Test
    void getUserOrders_returnsOrderList() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user1");

        User user = new User();
        user.setId(10L);

        Cafe cafe = new Cafe();
        cafe.setName("CafeTest");

        MenuItem item1 = new MenuItem();
        item1.setName("Coffee");

        MenuItem item2 = new MenuItem();
        item2.setName("Cake");

        Order order = new Order();
        order.setId(5L);
        order.setCafe(cafe);
        order.setTotalAmount(15.0);
        order.setItems(List.of(item1, item2));

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserId(user.getId())).thenReturn(List.of(order));

        ResponseEntity<List<Map<String, Object>>> response = customerController.getUserOrders(auth);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        List<Map<String, Object>> orders = response.getBody();

        assertThat(orders).hasSize(1);
        Map<String, Object> orderMap = orders.get(0);
        assertThat(orderMap.get("id")).isEqualTo(5L);
        assertThat(orderMap.get("cafeName")).isEqualTo("CafeTest");
        assertThat(orderMap.get("totalAmount")).isEqualTo(15.0);
        assertThat(orderMap.get("itemNames")).isEqualTo(List.of("Coffee", "Cake"));
    }
}
