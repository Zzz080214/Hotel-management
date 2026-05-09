package com.yueqi.hotel;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.yueqi.hotel.entity.HotelOrder;
import com.yueqi.hotel.entity.RoomType;
import com.yueqi.hotel.dto.WxOrderCreateRequest;
import com.yueqi.hotel.repository.RoomTypeRepository;
import com.yueqi.hotel.repository.UserRepository;
import com.yueqi.hotel.service.OrderService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HotelManagementApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private UserRepository userRepository;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void wxAndAdminApisWork() {
        Map<?, ?> rooms = get("/api/wx/rooms");
        assertThat(rooms.get("code")).isEqualTo(200);
        assertThat(rooms.get("data")).asList().hasSizeGreaterThanOrEqualTo(4);
        Map<?, ?> deluxeKingRoom = ((List<?>) rooms.get("data")).stream()
                .map(item -> (Map<?, ?>) item)
                .filter(item -> "豪华大床房".equals(item.get("name")))
                .findFirst()
                .orElseThrow();
        assertThat(new BigDecimal(String.valueOf(deluxeKingRoom.get("price")))).isEqualByComparingTo("426");

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
                "guestIdCard", "440101199901019999",
                "stayNights", 2,
                "totalAmount", new BigDecimal("852"),
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

        Map<?, ?> paidOrder = post("/api/wx/orders/" + orderId + "/pay", Map.of(), wxTokenA);
        assertThat(((Map<?, ?>) paidOrder.get("data")).get("paymentStatus")).isEqualTo("paid");

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

    @Test
    void stayingOrdersAutoCheckoutAtCheckoutDateTwoPm() {
        String wxToken = wxLogin("wx-auto-checkout-user");
        RoomType roomBefore = roomTypeRepository.findById(2L).orElseThrow();
        int availableBefore = roomBefore.getAvailableRooms();

        LocalDate checkOutDate = LocalDate.now().plusDays(1);
        Map<String, Object> orderPayload = Map.of(
                "roomTypeId", 2,
                "roomTypeName", "标准双床房",
                "guestName", "自动退房住客",
                "guestPhone", "13800138010",
                "guestIdCard", "440101199902029999",
                "stayNights", 1,
                "totalAmount", new BigDecimal("396"),
                "checkInDate", LocalDate.now().toString(),
                "checkOutDate", checkOutDate.toString());
        Map<?, ?> createdOrder = post("/api/wx/orders", orderPayload, wxToken);
        String orderId = String.valueOf(((Map<?, ?>) createdOrder.get("data")).get("id"));

        post("/api/wx/orders/" + orderId + "/pay", Map.of(), wxToken);
        post("/api/wx/orders/" + orderId + "/face-check-in", Map.of(), wxToken);

        int beforeTwoPm = orderService.autoCheckoutOverdueOrders(checkOutDate.atTime(13, 59));
        HotelOrder stillStaying = orderService.getRequired(orderId);
        assertThat(beforeTwoPm).isZero();
        assertThat(stillStaying.getStatus()).isEqualTo("staying");

        int afterTwoPm = orderService.autoCheckoutOverdueOrders(checkOutDate.atTime(14, 1));
        HotelOrder finished = orderService.getRequired(orderId);
        RoomType roomAfter = roomTypeRepository.findById(2L).orElseThrow();

        assertThat(afterTwoPm).isEqualTo(1);
        assertThat(finished.getStatus()).isEqualTo("finished");
        assertThat(finished.getCheckOutSource()).isEqualTo("AUTO_CHECKOUT");
        assertThat(finished.getCheckOutAt()).isEqualTo(LocalDateTime.of(checkOutDate, java.time.LocalTime.of(14, 0)));
        assertThat(roomAfter.getAvailableRooms()).isEqualTo(availableBefore);
    }

    @Test
    void sameIdCardCannotBookOverlappingStayDates() {
        String wxToken = wxLogin("wx-duplicate-id-card-user");
        String idCard = "440101199903039999";
        LocalDate checkInDate = LocalDate.now().plusDays(6);

        Map<String, Object> firstOrder = Map.of(
                "roomTypeId", 1,
                "roomTypeName", "豪华大床房",
                "guestName", "重复住客",
                "guestPhone", "13800138011",
                "guestIdCard", idCard,
                "stayNights", 2,
                "totalAmount", new BigDecimal("852"),
                "checkInDate", checkInDate.toString(),
                "checkOutDate", checkInDate.plusDays(2).toString());
        post("/api/wx/orders", firstOrder, wxToken);

        Map<String, Object> secondOrder = Map.of(
                "roomTypeId", 2,
                "roomTypeName", "标准双床房",
                "guestName", "重复住客",
                "guestPhone", "13800138012",
                "guestIdCard", idCard,
                "stayNights", 1,
                "totalAmount", new BigDecimal("396"),
                "checkInDate", checkInDate.plusDays(1).toString(),
                "checkOutDate", checkInDate.plusDays(2).toString());
        ResponseEntity<Map> duplicate = exchange("/api/wx/orders", HttpMethod.POST, secondOrder, wxToken);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void wxOrderAmountIsCalculatedOnServerAndPaidOnlyAfterConfirmation() {
        String wxToken = wxLogin("wx-server-pricing-user");
        LocalDate checkInDate = LocalDate.now().plusDays(12);
        Map<String, Object> forgedPayload = mutableOrderPayload(
                1,
                "豪华大床房",
                "金额伪造住客",
                "13800138120",
                "440101199905059999",
                2,
                new BigDecimal("0.01"),
                checkInDate,
                checkInDate.plusDays(2));
        forgedPayload.put("originalAmount", new BigDecimal("0.01"));
        forgedPayload.put("discountAmount", new BigDecimal("9999.00"));
        forgedPayload.put("couponId", "coupon-stay");
        forgedPayload.put("couponTitle", "前端伪造优惠券");
        forgedPayload.put("paymentStatus", "paid");

        Map<?, ?> created = post("/api/wx/orders", forgedPayload, wxToken);
        Map<?, ?> order = (Map<?, ?>) created.get("data");
        String orderId = String.valueOf(order.get("id"));

        assertThat(new BigDecimal(String.valueOf(order.get("originalAmount")))).isEqualByComparingTo("852.00");
        assertThat(new BigDecimal(String.valueOf(order.get("discountAmount")))).isEqualByComparingTo("42.60");
        assertThat(new BigDecimal(String.valueOf(order.get("totalAmount")))).isEqualByComparingTo("809.40");
        assertThat(order.get("couponId")).isEqualTo("coupon-stay");
        assertThat(order.get("couponTitle")).isEqualTo("连住优惠券");
        assertThat(order.get("paymentStatus")).isEqualTo("pending");
        assertThat(order.get("paidAt")).isNull();

        ResponseEntity<Map> checkInBeforePayment = exchange(
                "/api/wx/orders/" + orderId + "/face-check-in",
                HttpMethod.POST,
                Map.of(),
                wxToken);
        assertThat(checkInBeforePayment.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Map<?, ?> paid = post("/api/wx/orders/" + orderId + "/pay", Map.of(), wxToken);
        Map<?, ?> paidOrder = (Map<?, ?>) paid.get("data");
        assertThat(paidOrder.get("paymentStatus")).isEqualTo("paid");
        assertThat(new BigDecimal(String.valueOf(paidOrder.get("totalAmount")))).isEqualByComparingTo("809.40");
        assertThat(paidOrder.get("paidAt")).isNotNull();
    }

    @Test
    void paymentCallbackMustMatchServerCalculatedAmount() {
        String wxToken = wxLogin("wx-callback-user");
        LocalDate checkInDate = LocalDate.now().plusDays(16);
        Map<String, Object> payload = mutableOrderPayload(
                1,
                "豪华大床房",
                "支付回调住客",
                "13800138121",
                "440101199906069999",
                2,
                new BigDecimal("999.00"),
                checkInDate,
                checkInDate.plusDays(2));
        payload.put("couponId", "coupon-stay");
        Map<?, ?> created = post("/api/wx/orders", payload, wxToken);
        String orderId = String.valueOf(((Map<?, ?>) created.get("data")).get("id"));

        ResponseEntity<Map> wrongAmount = postPaymentCallback(Map.of(
                "orderId", orderId,
                "amount", new BigDecimal("0.01"),
                "transactionId", "PAY-WRONG-AMOUNT",
                "status", "SUCCESS"));
        assertThat(wrongAmount.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<Map> exactAmount = postPaymentCallback(Map.of(
                "orderId", orderId,
                "amount", new BigDecimal("809.40"),
                "transactionId", "PAY-EXACT-AMOUNT",
                "status", "SUCCESS"));
        assertThat(exactAmount.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> paidOrder = (Map<?, ?>) exactAmount.getBody().get("data");
        assertThat(paidOrder.get("paymentStatus")).isEqualTo("paid");
        assertThat(paidOrder.get("paymentTradeNo")).isEqualTo("PAY-EXACT-AMOUNT");
    }

    @Test
    void disabledBackOfficeUserTokenIsRejected() {
        String staffToken = login("staff", "staff123");
        var staff = userRepository.findByUsername("staff").orElseThrow();

        try {
            staff.setEnabled(false);
            userRepository.save(staff);

            ResponseEntity<Map> response = exchange("/api/admin/orders", HttpMethod.GET, null, staffToken);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        } finally {
            var restoredStaff = userRepository.findByUsername("staff").orElseThrow();
            restoredStaff.setEnabled(true);
            userRepository.save(restoredStaff);
        }
    }

    @Test
    void defaultBackOfficePasswordsAreStoredAsHashes() {
        var admin = userRepository.findByUsername("admin").orElseThrow();
        assertThat(admin.getPassword()).isNotEqualTo("admin123");
        assertThat(admin.getPassword()).startsWith("$2");
    }

    @Test
    void concurrentBookingCannotOversellSingleAvailableRoom() throws Exception {
        RoomType roomType = new RoomType();
        roomType.setName("并发测试房");
        roomType.setPrice(new BigDecimal("299"));
        roomType.setArea("25m²");
        roomType.setBed("1张 1.5m×2.0m");
        roomType.setBreakfast("无");
        roomType.setOccupancy("2人");
        roomType.setStatus("steady");
        roomType.setTag("测试");
        roomType.setImage("");
        roomType.setSummary("并发库存测试专用房型");
        roomType.setTotalRooms(1);
        roomType.setAvailableRooms(1);
        roomType.setEnabled(true);
        RoomType savedRoomType = roomTypeRepository.save(roomType);

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failedCount = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        LocalDate checkInDate = LocalDate.now().plusDays(20);

        for (int i = 0; i < 2; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    start.await(5, TimeUnit.SECONDS);
                    orderService.createOrderForWx(new WxOrderCreateRequest(
                            savedRoomType.getId(),
                            savedRoomType.getName(),
                            "并发住客" + index,
                            "1380013810" + index,
                            "44010119990404000" + index,
                            1,
                            new BigDecimal("299"),
                            null,
                            null,
                            null,
                            null,
                            "paid",
                            checkInDate,
                            checkInDate.plusDays(1)), "wx-concurrent-" + index);
                    successCount.incrementAndGet();
                } catch (Exception exception) {
                    failedCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();

        RoomType afterBooking = roomTypeRepository.findById(savedRoomType.getId()).orElseThrow();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failedCount.get()).isEqualTo(1);
        assertThat(afterBooking.getAvailableRooms()).isZero();
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

    private ResponseEntity<Map> postPaymentCallback(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Payment-Callback-Secret", "test-payment-callback-secret");
        return restTemplate.exchange(
                url("/api/payments/wechat/callback"),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);
    }

    private Map<String, Object> mutableOrderPayload(
            long roomTypeId,
            String roomTypeName,
            String guestName,
            String guestPhone,
            String guestIdCard,
            int stayNights,
            BigDecimal totalAmount,
            LocalDate checkInDate,
            LocalDate checkOutDate) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("roomTypeId", roomTypeId);
        payload.put("roomTypeName", roomTypeName);
        payload.put("guestName", guestName);
        payload.put("guestPhone", guestPhone);
        payload.put("guestIdCard", guestIdCard);
        payload.put("stayNights", stayNights);
        payload.put("totalAmount", totalAmount);
        payload.put("checkInDate", checkInDate.toString());
        payload.put("checkOutDate", checkOutDate.toString());
        return payload;
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
        String token = String.valueOf(data.get("token"));
        assertThat(token.split("\\.")).hasSize(3);
        return token;
    }

    private String adminLogin() {
        return login("admin", "admin123");
    }

    private String login(String username, String password) {
        Map<?, ?> response = post("/api/auth/login", Map.of(
                "username", username,
                "password", password));
        Map<?, ?> data = (Map<?, ?>) response.get("data");
        String token = String.valueOf(data.get("token"));
        assertThat(token.split("\\.")).hasSize(3);
        return token;
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
