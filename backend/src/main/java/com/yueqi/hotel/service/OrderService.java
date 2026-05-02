package com.yueqi.hotel.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.yueqi.hotel.dto.CheckInRequest;
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

        RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
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
        order.setStayNights(request.stayNights());
        order.setTotalAmount(resolveAmount(request, roomType));
        order.setCheckInDate(request.checkInDate());
        order.setCheckOutDate(request.checkOutDate());
        order.setStatus(STATUS_UPCOMING);
        order.setCreatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<HotelOrder> listMyOrders(String status, String wxOpenid) {
        if (!StringUtils.hasText(wxOpenid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后查看订单");
        }
        if (StringUtils.hasText(status) && !"all".equals(status)) {
            return orderRepository.findByWxOpenidAndStatusOrderByCreatedAtDesc(wxOpenid, status);
        }
        return orderRepository.findByWxOpenidOrderByCreatedAtDesc(wxOpenid);
    }

    @Transactional(readOnly = true)
    public List<HotelOrder> adminList(String keyword, String status) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String normalizedStatus = status == null ? "" : status.trim();
        return orderRepository.findAllNewestFirst().stream()
                .filter(order -> !StringUtils.hasText(normalizedStatus)
                        || "all".equals(normalizedStatus)
                        || normalizedStatus.equals(order.getStatus()))
                .filter(order -> !StringUtils.hasText(normalizedKeyword)
                        || contains(order.getId(), normalizedKeyword)
                        || contains(order.getGuestName(), normalizedKeyword)
                        || contains(order.getRoomTypeName(), normalizedKeyword)
                        || contains(order.getGuestPhone(), normalizedKeyword))
                .toList();
    }

    @Transactional(readOnly = true)
    public HotelOrder getRequired(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
    }

    @Transactional(readOnly = true)
    public HotelOrder getRequiredForWx(String id, String wxOpenid) {
        if (!StringUtils.hasText(wxOpenid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后查看订单");
        }
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
            releaseRoom(order);
        }
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public long countByStatus(String status) {
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

    @Transactional(readOnly = true)
    public BigDecimal todayRevenue() {
        LocalDate today = LocalDate.now();
        return orderRepository.findAllNewestFirst().stream()
                .filter(order -> today.equals(order.getCheckInDate()) || today.equals(order.getCheckOutDate()))
                .filter(order -> !STATUS_CANCELLED.equals(order.getStatus()))
                .map(HotelOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal resolveAmount(WxOrderCreateRequest request, RoomType roomType) {
        if (request.totalAmount() != null && request.totalAmount().compareTo(BigDecimal.ZERO) > 0) {
            return request.totalAmount();
        }
        return roomType.getPrice().multiply(BigDecimal.valueOf(request.stayNights()));
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
        long todayCount = orderRepository.countToday() + 1;
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "HT" + datePart + String.format("%03d", todayCount);
    }
}
