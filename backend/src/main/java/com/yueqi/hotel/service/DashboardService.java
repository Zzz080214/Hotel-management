package com.yueqi.hotel.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yueqi.hotel.entity.HotelOrder;
import com.yueqi.hotel.entity.RoomType;

@Service
public class DashboardService {

    private final RoomTypeService roomTypeService;
    private final OrderService orderService;
    private final NoticeService noticeService;

    public DashboardService(RoomTypeService roomTypeService, OrderService orderService, NoticeService noticeService) {
        this.roomTypeService = roomTypeService;
        this.orderService = orderService;
        this.noticeService = noticeService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> overview() {
        List<RoomType> rooms = roomTypeService.listEnabled();
        int totalRooms = rooms.stream().mapToInt(room -> value(room.getTotalRooms())).sum();
        int availableRooms = rooms.stream().mapToInt(room -> value(room.getAvailableRooms())).sum();
        int occupiedRooms = Math.max(0, totalRooms - availableRooms);
        BigDecimal todayRevenue = orderService.todayRevenue();

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("occupancyRate", percent(occupiedRooms, totalRooms));
        metrics.put("todayOrders", orderService.countByStatus("upcoming"));
        metrics.put("todayRevenue", todayRevenue);
        metrics.put("pendingCheckout", orderService.countByStatus("staying"));
        metrics.put("exceptionOrders", orderService.countByStatus("cancelled"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("metrics", metrics);
        data.put("roomTypes", rooms);
        data.put("latestOrders", orderService.adminList(null, null).stream().limit(5).toList());
        data.put("notices", noticeService.listPublished().stream().limit(5).toList());
        return data;
    }

    @Transactional(readOnly = true)
    public String operationsReportCsv() {
        List<RoomType> rooms = roomTypeService.listEnabled();
        List<HotelOrder> orders = orderService.adminList(null, null);

        int totalRooms = rooms.stream().mapToInt(room -> value(room.getTotalRooms())).sum();
        int availableRooms = rooms.stream().mapToInt(room -> value(room.getAvailableRooms())).sum();
        int occupiedRooms = Math.max(0, totalRooms - availableRooms);
        BigDecimal totalRevenue = orders.stream()
                .filter(order -> !"cancelled".equals(order.getStatus()))
                .map(order -> order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder csv = new StringBuilder();
        appendRow(csv, "悦栖酒店经营统计报表");
        appendRow(csv, "导出时间", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        appendRow(csv);

        appendRow(csv, "核心指标", "数值");
        appendRow(csv, "总房间数", totalRooms);
        appendRow(csv, "可用房间数", availableRooms);
        appendRow(csv, "当前在住房间数", occupiedRooms);
        appendRow(csv, "当前入住率", percent(occupiedRooms, totalRooms));
        appendRow(csv, "有效订单数", orders.stream().filter(order -> !"cancelled".equals(order.getStatus())).count());
        appendRow(csv, "取消订单数", orderService.countByStatus("cancelled"));
        appendRow(csv, "累计有效营收", totalRevenue);
        appendRow(csv);

        appendRow(csv, "房型表现");
        appendRow(csv, "房型", "单价", "总房间", "可用房间", "已占用", "入住率", "状态");
        for (RoomType room : rooms) {
            int roomTotal = value(room.getTotalRooms());
            int roomAvailable = value(room.getAvailableRooms());
            int roomOccupied = Math.max(0, roomTotal - roomAvailable);
            appendRow(csv,
                    room.getName(),
                    room.getPrice(),
                    roomTotal,
                    roomAvailable,
                    roomOccupied,
                    percent(roomOccupied, roomTotal),
                    room.getStatus());
        }
        appendRow(csv);

        appendRow(csv, "近期订单");
        appendRow(csv, "订单号", "房型", "入住日期", "退房日期", "晚数", "金额", "状态");
        orders.stream().limit(20).forEach(order -> appendRow(csv,
                order.getId(),
                order.getRoomTypeName(),
                order.getCheckInDate(),
                order.getCheckOutDate(),
                order.getStayNights(),
                order.getTotalAmount(),
                statusText(order.getStatus())));

        return csv.toString();
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String percent(int numerator, int denominator) {
        if (denominator <= 0) {
            return "0%";
        }
        double rate = numerator * 100.0 / denominator;
        return String.format("%.1f%%", rate);
    }

    private String statusText(String status) {
        return switch (status == null ? "" : status) {
            case "upcoming" -> "待入住";
            case "staying" -> "在住";
            case "finished" -> "已完成";
            case "cancelled" -> "已取消";
            default -> status;
        };
    }

    private void appendRow(StringBuilder csv, Object... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escapeCsv(cells[i]));
        }
        csv.append("\r\n");
    }

    private String escapeCsv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (text.contains("\"") || text.contains(",") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
