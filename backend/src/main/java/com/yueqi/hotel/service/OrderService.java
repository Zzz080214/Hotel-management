package com.yueqi.hotel.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.yueqi.hotel.dto.CheckInRequest;
import com.yueqi.hotel.dto.PaymentCallbackRequest;
import com.yueqi.hotel.dto.WxOrderCreateRequest;
import com.yueqi.hotel.entity.HotelOrder;
import com.yueqi.hotel.entity.RoomType;
import com.yueqi.hotel.repository.HotelOrderRepository;
import com.yueqi.hotel.repository.RoomTypeRepository;

@Service
public class OrderService {

    private static final String STATUS_UPCOMING = "upcoming";
    private static final String STATUS_STAYING = "staying";
    private static final String STATUS_FINISHED = "finished";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final String PAYMENT_STATUS_PENDING = "pending";
    private static final String PAYMENT_STATUS_PAID = "paid";
    private static final String PAYMENT_STATUS_REFUNDED = "refunded";
    private static final String REFUND_STATUS_REFUNDED = "refunded";
    private static final LocalTime AUTO_CHECKOUT_TIME = LocalTime.of(14, 0);
    private static final String NO_COUPON_ID = "none";

    private final HotelOrderRepository orderRepository;
    private final RoomTypeRepository roomTypeRepository;

    public OrderService(HotelOrderRepository orderRepository, RoomTypeRepository roomTypeRepository) {
        this.orderRepository = orderRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @Transactional
    public HotelOrder createOrder(WxOrderCreateRequest request) {
        return createOrderInternal(request, null, request.guestPhone());
    }

    @Transactional
    public HotelOrder createOrderForWx(WxOrderCreateRequest request, String wxOpenid) {
        if (!StringUtils.hasText(wxOpenid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先微信登录后再预订");
        }
        return createOrderInternal(request, wxOpenid, request.guestPhone());
    }

    private HotelOrder createOrderInternal(WxOrderCreateRequest request, String wxOpenid, String userPhone) {
        if (!request.checkOutDate().isAfter(request.checkInDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "退房日期必须晚于入住日期");
        }
        String guestIdCard = request.guestIdCard().trim().toUpperCase(Locale.ROOT);
        assertNoOverlappingIdCardBooking(guestIdCard, request.checkInDate(), request.checkOutDate());

        RoomType roomType = roomTypeRepository.findByIdForUpdate(request.roomTypeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "房型不存在"));
        if (!Boolean.TRUE.equals(roomType.getEnabled())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "房型已下架");
        }
        if (roomType.getAvailableRooms() == null || roomType.getAvailableRooms() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前房型暂无可订库存");
        }

        roomType.setAvailableRooms(roomType.getAvailableRooms() - 1);
        roomTypeRepository.save(roomType);

        HotelOrder order = new HotelOrder();
        order.setId(nextOrderNo());
        order.setWxOpenid(wxOpenid);
        order.setUserPhone(userPhone);
        order.setRoomTypeId(roomType.getId());
        order.setRoomTypeName(StringUtils.hasText(request.roomTypeName()) ? request.roomTypeName() : roomType.getName());
        order.setGuestName(request.guestName());
        order.setGuestPhone(request.guestPhone());
        order.setGuestIdCard(guestIdCard);
        order.setStayNights(request.stayNights());
        Pricing pricing = calculatePricing(request, roomType);
        order.setTotalAmount(pricing.payableAmount());
        order.setOriginalAmount(pricing.originalAmount());
        order.setDiscountAmount(pricing.discountAmount());
        order.setCouponId(pricing.couponId());
        order.setCouponTitle(pricing.couponTitle());
        order.setPaymentStatus(PAYMENT_STATUS_PENDING);
        order.setCheckInDate(request.checkInDate());
        order.setCheckOutDate(request.checkOutDate());
        order.setStatus(STATUS_UPCOMING);
        order.setCreatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @Transactional
    public List<HotelOrder> listMyOrders(String status, String wxOpenid) {
        if (!StringUtils.hasText(wxOpenid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后查看订单");
        }
        autoCheckoutOverdueOrders(LocalDateTime.now());
        if (StringUtils.hasText(status) && !"all".equals(status)) {
            return orderRepository.findByWxOpenidAndStatusOrderByCreatedAtDesc(wxOpenid, status);
        }
        return orderRepository.findByWxOpenidOrderByCreatedAtDesc(wxOpenid);
    }

    @Transactional
    public List<HotelOrder> adminList(String keyword, String status) {
        autoCheckoutOverdueOrders(LocalDateTime.now());
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String normalizedStatus = status == null ? "" : status.trim();
        return orderRepository.findAllNewestFirst().stream()
                .filter(order -> !StringUtils.hasText(normalizedStatus)
                        || "all".equals(normalizedStatus)
                        || normalizedStatus.equals(order.getStatus()))
                .filter(order -> !StringUtils.hasText(normalizedKeyword)
                        || contains(order.getId(), normalizedKeyword)
                        || contains(order.getGuestName(), normalizedKeyword)
                        || contains(order.getGuestIdCard(), normalizedKeyword)
                        || contains(order.getRoomTypeName(), normalizedKeyword)
                        || contains(order.getGuestPhone(), normalizedKeyword))
                .toList();
    }

    @Transactional(readOnly = true)
    public HotelOrder getRequired(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
    }

    @Transactional
    public HotelOrder getRequiredForWx(String id, String wxOpenid) {
        if (!StringUtils.hasText(wxOpenid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后查看订单");
        }
        autoCheckoutOverdueOrders(LocalDateTime.now());
        HotelOrder order = getRequired(id);
        if (!wxOpenid.equals(order.getWxOpenid())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    @Transactional
    public HotelOrder checkIn(String id, CheckInRequest request) {
        HotelOrder order = getRequired(id);
        if (STATUS_FINISHED.equals(order.getStatus()) || STATUS_CANCELLED.equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前订单不能办理入住");
        }
        if (!PAYMENT_STATUS_PAID.equals(order.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先完成支付后再办理入住");
        }
        order.setStatus(STATUS_STAYING);
        order.setRoomNo(request.roomNo());
        order.setCheckInAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @Transactional
    public HotelOrder selfCheckInForWx(String id, String wxOpenid) {
        HotelOrder order = getRequiredForWx(id, wxOpenid);
        if (STATUS_FINISHED.equals(order.getStatus()) || STATUS_CANCELLED.equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前订单不能办理自助入住");
        }
        if (!PAYMENT_STATUS_PAID.equals(order.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先完成支付后再办理入住");
        }
        if (!StringUtils.hasText(order.getRoomNo())) {
            order.setRoomNo(generateDemoRoomNo(order));
        }
        order.setStatus(STATUS_STAYING);
        order.setCheckInAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    /** 前台员工操作退房 */
    @Transactional
    public HotelOrder checkOut(String id) {
        return checkOutInternal(id, "FRONT_DESK");
    }

    /** 微信小程序端自动退房 */
    @Transactional
    public HotelOrder selfCheckOutForWx(String id, String wxOpenid) {
        if (!StringUtils.hasText(wxOpenid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后操作");
        }
        HotelOrder order = getRequired(id);
        if (!wxOpenid.equals(order.getWxOpenid())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
        }
        if (!STATUS_STAYING.equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前订单尚未入住，不能自助退房");
        }
        return checkOutInternal(id, "AUTO_CHECKOUT");
    }

    private HotelOrder checkOutInternal(String id, String source) {
        HotelOrder order = getRequired(id);
        if (STATUS_CANCELLED.equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "已取消订单不能退房");
        }
        if (!STATUS_FINISHED.equals(order.getStatus())) {
            order.setStatus(STATUS_FINISHED);
            order.setCheckOutAt(LocalDateTime.now());
            order.setCheckOutSource(source);
            releaseRoom(order);
        }
        return orderRepository.save(order);
    }

    @Transactional
    public HotelOrder cancel(String id) {
        HotelOrder order = getRequired(id);
        if (STATUS_FINISHED.equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "已完成订单不能取消");
        }
        if (!STATUS_CANCELLED.equals(order.getStatus())) {
            order.setStatus(STATUS_CANCELLED);
            applyRefundIfPaid(order);
            releaseRoom(order);
        }
        return orderRepository.save(order);
    }

    @Transactional
    public HotelOrder cancelForWx(String id, String wxOpenid) {
        HotelOrder order = getRequiredForWx(id, wxOpenid);
        if (STATUS_FINISHED.equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "已完成订单不能取消");
        }
        if (STATUS_STAYING.equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前订单已入住，不能取消");
        }
        if (!STATUS_CANCELLED.equals(order.getStatus())) {
            order.setStatus(STATUS_CANCELLED);
            applyRefundIfPaid(order);
            releaseRoom(order);
        }
        return orderRepository.save(order);
    }

    @Transactional
    public HotelOrder payForWx(String id, String wxOpenid) {
        HotelOrder order = getRequiredForWx(id, wxOpenid);
        return confirmPayment(order);
    }

    @Transactional
    public HotelOrder confirmPaymentFromAdmin(String id) {
        HotelOrder order = getRequired(id);
        return confirmPayment(order);
    }

    @Transactional
    public HotelOrder confirmPaymentCallback(PaymentCallbackRequest request) {
        if (!"SUCCESS".equalsIgnoreCase(request.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "支付未成功，不能确认订单");
        }
        HotelOrder order = getRequired(request.orderId());
        if (request.amount().compareTo(order.getTotalAmount()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "支付金额与订单应付金额不一致");
        }
        if (StringUtils.hasText(order.getPaymentTradeNo())
                && !order.getPaymentTradeNo().equals(request.transactionId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "订单已绑定其他支付流水");
        }
        order.setPaymentTradeNo(request.transactionId());
        return confirmPayment(order);
    }

    private HotelOrder confirmPayment(HotelOrder order) {
        if (!STATUS_UPCOMING.equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前订单不能支付");
        }
        if (PAYMENT_STATUS_REFUNDED.equals(order.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "已退款订单不能支付");
        }
        if (!PAYMENT_STATUS_PAID.equals(order.getPaymentStatus())) {
            order.setPaymentStatus(PAYMENT_STATUS_PAID);
            order.setPaidAt(LocalDateTime.now());
        }
        return orderRepository.save(order);
    }

    @Transactional
    public long countByStatus(String status) {
        autoCheckoutOverdueOrders(LocalDateTime.now());
        return orderRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return orderRepository.count();
    }

    @Transactional(readOnly = true)
    public long countByWxOpenid(String wxOpenid) {
        if (!StringUtils.hasText(wxOpenid)) {
            return 0;
        }
        return orderRepository.findByWxOpenidOrderByCreatedAtDesc(wxOpenid).size();
    }

    @Transactional
    public BigDecimal todayRevenue() {
        autoCheckoutOverdueOrders(LocalDateTime.now());
        LocalDate today = LocalDate.now();
        return orderRepository.findAllNewestFirst().stream()
                .filter(order -> today.equals(order.getCheckInDate()) || today.equals(order.getCheckOutDate()))
                .filter(order -> !STATUS_CANCELLED.equals(order.getStatus()))
                .map(HotelOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public int autoCheckoutOverdueOrders(LocalDateTime now) {
        if (now == null) {
            return 0;
        }
        List<HotelOrder> overdueOrders = orderRepository
                .findByStatusAndCheckOutDateLessThanEqual(STATUS_STAYING, now.toLocalDate());
        int count = 0;
        for (HotelOrder order : overdueOrders) {
            if (order.getCheckOutDate() == null
                    || order.getCheckOutDate().atTime(AUTO_CHECKOUT_TIME).isAfter(now)) {
                continue;
            }
            order.setStatus(STATUS_FINISHED);
            order.setCheckOutAt(order.getCheckOutDate().atTime(AUTO_CHECKOUT_TIME));
            order.setCheckOutSource("AUTO_CHECKOUT");
            releaseRoom(order);
            orderRepository.save(order);
            count += 1;
        }
        return count;
    }

    private void assertNoOverlappingIdCardBooking(String guestIdCard, LocalDate checkInDate, LocalDate checkOutDate) {
        boolean hasConflict = orderRepository.findByGuestIdCardOrderByCreatedAtDesc(guestIdCard).stream()
                .filter(order -> !STATUS_CANCELLED.equals(order.getStatus()))
                .anyMatch(order -> datesOverlap(checkInDate, checkOutDate, order.getCheckInDate(), order.getCheckOutDate()));
        if (hasConflict) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该身份证在入住日期内已有订单，同一天不能重复预订两个房间");
        }
    }

    private boolean datesOverlap(LocalDate startA, LocalDate endA, LocalDate startB, LocalDate endB) {
        if (startA == null || endA == null || startB == null || endB == null) {
            return false;
        }
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    private Pricing calculatePricing(WxOrderCreateRequest request, RoomType roomType) {
        BigDecimal originalAmount = money(roomType.getPrice().multiply(BigDecimal.valueOf(request.stayNights())));
        CouponRule coupon = resolveCouponRule(request.couponId());
        if (coupon == null) {
            return new Pricing(originalAmount, BigDecimal.ZERO.setScale(2), originalAmount, null, null);
        }
        if (!coupon.isEligible(roomType, request.stayNights(), originalAmount)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "优惠券不满足使用条件");
        }
        BigDecimal discountAmount = coupon.calculateDiscount(originalAmount);
        BigDecimal payableAmount = money(originalAmount.subtract(discountAmount).max(BigDecimal.ZERO));
        return new Pricing(originalAmount, discountAmount, payableAmount, coupon.id(), coupon.title());
    }

    private CouponRule resolveCouponRule(String couponId) {
        if (!StringUtils.hasText(couponId) || NO_COUPON_ID.equals(couponId)) {
            return null;
        }
        return switch (couponId) {
            case "coupon-new" -> new CouponRule(
                    "coupon-new",
                    "新客立减券",
                    CouponType.FIXED,
                    new BigDecimal("60.00"),
                    new BigDecimal("399.00"),
                    null);
            case "coupon-stay" -> new CouponRule(
                    "coupon-stay",
                    "连住优惠券",
                    CouponType.PERCENT,
                    new BigDecimal("0.95"),
                    null,
                    2);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "优惠券不存在或不可用于支付");
        };
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private void applyRefundIfPaid(HotelOrder order) {
        if (!PAYMENT_STATUS_PAID.equals(order.getPaymentStatus())) {
            return;
        }
        order.setPaymentStatus(PAYMENT_STATUS_REFUNDED);
        order.setRefundStatus(REFUND_STATUS_REFUNDED);
        order.setRefundAmount(order.getTotalAmount());
        order.setRefundedAt(LocalDateTime.now());
    }

    private void releaseRoom(HotelOrder order) {
        if (order.getRoomTypeId() == null) {
            return;
        }
        roomTypeRepository.findById(order.getRoomTypeId()).ifPresent(roomType -> {
            int totalRooms = roomType.getTotalRooms() == null ? 0 : roomType.getTotalRooms();
            int availableRooms = roomType.getAvailableRooms() == null ? 0 : roomType.getAvailableRooms();
            roomType.setAvailableRooms(Math.min(totalRooms, availableRooms + 1));
            roomTypeRepository.save(roomType);
        });
    }

    private boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String generateDemoRoomNo(HotelOrder order) {
        int prefix = switch (order.getRoomTypeName() == null ? "" : order.getRoomTypeName()) {
            case "豪华大床房" -> 8100;
            case "商务双床房", "标准双床房" -> 8200;
            case "亲子家庭房" -> 8600;
            case "行政套房", "行政大床房" -> 8800;
            case "钟点房" -> 8500;
            default -> 8000;
        };
        return String.valueOf(prefix + Math.floorMod(order.getId().hashCode(), 48) + 1);
    }

    private String nextOrderNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmssSSS"));
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        return "HT" + datePart + timePart + randomPart;
    }

    private enum CouponType {
        FIXED,
        PERCENT
    }

    private record CouponRule(
            String id,
            String title,
            CouponType type,
            BigDecimal value,
            BigDecimal minAmount,
            Integer minNights) {

        boolean isEligible(RoomType roomType, Integer stayNights, BigDecimal originalAmount) {
            if (minAmount != null && originalAmount.compareTo(minAmount) < 0) {
                return false;
            }
            return minNights == null || stayNights >= minNights;
        }

        BigDecimal calculateDiscount(BigDecimal originalAmount) {
            BigDecimal discount = switch (type) {
                case FIXED -> value;
                case PERCENT -> originalAmount.multiply(BigDecimal.ONE.subtract(value));
            };
            return discount.min(originalAmount).setScale(2, RoundingMode.HALF_UP);
        }
    }

    private record Pricing(
            BigDecimal originalAmount,
            BigDecimal discountAmount,
            BigDecimal payableAmount,
            String couponId,
            String couponTitle) {
    }
}
