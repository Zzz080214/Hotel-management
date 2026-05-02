package com.yueqi.hotel;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HotelManagementApplicationTests {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void wxAndAdminApisWork() {
        Map<?, ?> rooms = get("/api/wx/rooms");
        assertThat(rooms.get("code")).isEqualTo(200);
        assertThat(rooms.get("data")).asList().hasSizeGreaterThanOrEqualTo(4);

        Map<?, ?> notices = get("/api/wx/notices");
        assertThat(notices.get("code")).isEqualTo(200);
        assertThat(notices.get("data")).asList().isNotEmpty();

        String wxTokenA = wxLogin("wx-user-a");
        String wxTokenB = wxLogin("wx-user-b");

        Map<String, Object> orderPayload = Map.of(
                "roomTypeId", 1,
                "roomTypeName", "豪华大床房",
                "guestName", "测试住客",
                "guestPhone", "13800138009",
                "stayNights", 2,
                "totalAmount", new BigDecimal("856"),
                "checkInDate", LocalDate.now().toString(),
                "checkOutDate", LocalDate.now().plusDays(2).toString());
        Map<?, ?> createdOrder = post("/api/wx/orders", orderPayload, wxTokenA);
        assertThat(createdOrder.get("code")).isEqualTo(200);
        Map<?, ?> orderData = (Map<?, ?>) createdOrder.get("data");
        String orderId = String.valueOf(orderData.get("id"));
        assertThat(orderId).startsWith("HT");

        Map<?, ?> myOrders = get("/api/wx/orders/my?status=upcoming", wxTokenA);
        assertThat(orderIds(myOrders)).contains(orderId);

        Map<?, ?> otherUserOrders = get("/api/wx/orders/my", wxTokenB);
        assertThat(orderIds(otherUserOrders)).doesNotContain(orderId);
        ResponseEntity<Map> otherUserDetail = exchange("/api/wx/orders/" + orderId, HttpMethod.GET, null, wxTokenB);
        assertThat(otherUserDetail.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        Map<?, ?> ownDetail = get("/api/wx/orders/" + orderId, wxTokenA);
        assertThat(((Map<?, ?>) ownDetail.get("data")).get("id")).isEqualTo(orderId);

        ResponseEntity<Map> otherUserCheckIn = exchange("/api/wx/orders/" + orderId + "/face-check-in", HttpMethod.POST, Map.of(), wxTokenB);
        assertThat(otherUserCheckIn.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        Map<?, ?> selfCheckedIn = post("/api/wx/orders/" + orderId + "/face-check-in", Map.of(), wxTokenA);
        Map<?, ?> selfCheckedInData = (Map<?, ?>) selfCheckedIn.get("data");
        assertThat(selfCheckedInData.get("id")).isEqualTo(orderId);
        assertThat(selfCheckedInData.get("status")).isEqualTo("staying");
        assertThat(String.valueOf(selfCheckedInData.get("roomNo"))).isNotBlank();

        ResponseEntity<Map> otherUserCheckOut = exchange("/api/wx/orders/" + orderId + "/self-check-out", HttpMethod.POST, Map.of(), wxTokenB);
        assertThat(otherUserCheckOut.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        Map<?, ?> selfCheckedOut = post("/api/wx/orders/" + orderId + "/self-check-out", Map.of(), wxTokenA);
        Map<?, ?> selfCheckedOutData = (Map<?, ?>) selfCheckedOut.get("data");
        assertThat(selfCheckedOutData.get("id")).isEqualTo(orderId);
        assertThat(selfCheckedOutData.get("status")).isEqualTo("finished");
        assertThat(selfCheckedOutData.get("checkOutSource")).isEqualTo("AUTO_CHECKOUT");

        Map<?, ?> profile = get("/api/wx/users/profile", wxTokenA);
        assertThat(((Map<?, ?>) profile.get("data")).get("nickname")).isEqualTo("酒店住客");

        String adminToken = adminLogin();
        Map<?, ?> dashboard = get("/api/admin/dashboard", adminToken);
        assertThat(((Map<?, ?>) dashboard.get("data")).get("metrics")).isInstanceOf(Map.class);
    }

    private Map<?, ?> get(String path) {
        return get(path, null);
    }

    private Map<?, ?> get(String path, String token) {
        ResponseEntity<Map> response = exchange(path, HttpMethod.GET, null, token);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    private Map<?, ?> post(String path, Object body) {
        return post(path, body, null);
    }

    private Map<?, ?> post(String path, Object body, String token) {
        ResponseEntity<Map> response = exchange(path, HttpMethod.POST, body, token);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    private ResponseEntity<Map> exchange(String path, HttpMethod method, Object body, String token) {
        return restTemplate.exchange(url(path), method, entity(body, token), Map.class);
    }

    private HttpEntity<Object> entity(Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token);
        }
        return new HttpEntity<>(body, headers);
    }

    private String wxLogin(String devOpenid) {
        Map<?, ?> response = post("/api/wx/auth/login", Map.of(
                "code", "code-" + devOpenid,
                "nickname", devOpenid,
                "avatarUrl", "",
                "devOpenid", devOpenid));
        Map<?, ?> data = (Map<?, ?>) response.get("data");
        assertThat(data.get("openid")).isEqualTo(devOpenid);
        return String.valueOf(data.get("token"));
    }

    private String adminLogin() {
        Map<?, ?> response = post("/api/auth/login", Map.of(
                "username", "admin",
                "password", "admin123"));
        Map<?, ?> data = (Map<?, ?>) response.get("data");
        return String.valueOf(data.get("token"));
    }

    private List<String> orderIds(Map<?, ?> response) {
        return ((List<?>) response.get("data")).stream()
                .map(item -> String.valueOf(((Map<?, ?>) item).get("id")))
                .toList();
    }

    private String url(String path) {
        return "http://127.0.0.1:" + port + path;
    }
}
