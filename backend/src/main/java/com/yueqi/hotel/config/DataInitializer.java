package com.yueqi.hotel.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.yueqi.hotel.entity.HotelOrder;
import com.yueqi.hotel.entity.Notice;
import com.yueqi.hotel.entity.RoomType;
import com.yueqi.hotel.entity.User;
import com.yueqi.hotel.entity.UserProfile;
import com.yueqi.hotel.repository.HotelOrderRepository;
import com.yueqi.hotel.repository.NoticeRepository;
import com.yueqi.hotel.repository.RoomTypeRepository;
import com.yueqi.hotel.repository.UserProfileRepository;
import com.yueqi.hotel.repository.UserRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(
            RoomTypeRepository roomTypeRepository,
            NoticeRepository noticeRepository,
            HotelOrderRepository orderRepository,
            UserProfileRepository userProfileRepository,
            UserRepository userRepository) {
        return args -> {
            if (roomTypeRepository.count() == 0) {
                roomTypeRepository.saveAll(List.of(
                        roomType("豪华大床房", "428", "32m²", "1张 1.8m×2.0m", "双早", "2人", "hot", "热门",
                                "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=900&q=80",
                                "高楼层景观房，适合情侣与商务单人入住。", 20, 8,
                                List.of("景观窗", "智能电视", "独立淋浴", "免费停车")),
                        roomType("标准双床房", "396", "35m²", "2张 1.35m×2.0m", "双早", "2人", "steady", "稳定",
                                "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?auto=format&fit=crop&w=900&q=80",
                                "双床布局更适合朋友、同事出行和双人旅行。", 18, 10,
                                List.of("静音楼层", "书桌办公", "行李架", "高速WiFi")),
                        roomType("行政大床房", "888", "58m²", "1张 2.0m×2.0m", "双早", "2人", "luxury", "高端",
                                "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=900&q=80",
                                "加宽大床与商务会客空间，适合高品质住宿和商务接待。", 8, 3,
                                List.of("会客沙发", "浴缸", "迷你吧", "欢迎水果")),
                        roomType("亲子家庭房", "168", "28m²", "1张 1.5m×2.0m + 1张 1.2m×2.0m", "无", "3人", "budget", "特惠",
                                "https://images.unsplash.com/photo-1445019980597-93fa8acb246c?auto=format&fit=crop&w=900&q=80",
                                "一大一小床组合，适合亲子家庭入住。", 16, 9,
                                List.of("亲子床型", "儿童用品", "宽敞空间", "卫生安心"))));
            }

            if (noticeRepository.count() == 0) {
                noticeRepository.save(notice("五一假期入住温馨提示", "重要", "节假日期间入住高峰较多，建议提前在小程序完成预订与登记。"));
                noticeRepository.save(notice("连住优惠活动上线", "活动", "连续入住两晚及以上可享95折，部分房型赠双早。"));
            }

            if (orderRepository.count() == 0) {
                String todayPrefix = "HT" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                orderRepository.save(order(todayPrefix + "001", 1L, "豪华大床房", "张同学", "13800138000",
                        2, "856", LocalDate.now(), LocalDate.now().plusDays(2), "upcoming", null));
                orderRepository.save(order(todayPrefix + "002", 1L, "豪华大床房", "赵女士", "13800138001",
                        3, "1284", LocalDate.now(), LocalDate.now().plusDays(3), "staying", "8105"));
                orderRepository.save(order(todayPrefix + "003", 4L, "亲子家庭房", "陈女士", "13800138002",
                        1, "168", LocalDate.now().minusDays(1), LocalDate.now().minusDays(1), "finished", "8601"));
            }
            orderRepository.findAll().forEach(order -> {
                if (order.getUserPhone() == null || order.getUserPhone().isBlank()) {
                    order.setUserPhone(order.getGuestPhone());
                }
                if (order.getWxOpenid() == null || order.getWxOpenid().isBlank()) {
                    order.setWxOpenid("dev-local-device");
                }
                orderRepository.save(order);
            });

            if (userProfileRepository.count() == 0) {
                UserProfile profile = new UserProfile();
                profile.setId(1L);
                profile.setNickname("酒店住客");
                profile.setDescription("欢迎再次入住悦栖酒店");
                profile.setCouponCount(2);
                profile.setMatchRate("93%");
                userProfileRepository.save(profile);
            }

            upsertUser(userRepository, "admin", "admin123", "ADMIN", "赵经理");
            upsertUser(userRepository, "staff", "staff123", "STAFF", "赵前台");
        };
    }

    private RoomType roomType(
            String name,
            String price,
            String area,
            String bed,
            String breakfast,
            String occupancy,
            String status,
            String tag,
            String image,
            String summary,
            Integer totalRooms,
            Integer availableRooms,
            List<String> features) {
        RoomType roomType = new RoomType();
        roomType.setName(name);
        roomType.setPrice(new BigDecimal(price));
        roomType.setArea(area);
        roomType.setBed(bed);
        roomType.setBreakfast(breakfast);
        roomType.setOccupancy(occupancy);
        roomType.setStatus(status);
        roomType.setTag(tag);
        roomType.setImage(image);
        roomType.setSummary(summary);
        roomType.setTotalRooms(totalRooms);
        roomType.setAvailableRooms(availableRooms);
        roomType.setEnabled(true);
        roomType.setFeatures(features);
        return roomType;
    }

    private Notice notice(String title, String level, String content) {
        Notice notice = new Notice();
        notice.setTitle(title);
        notice.setLevel(level);
        notice.setContent(content);
        notice.setPublished(true);
        notice.setPublishDate(LocalDate.now());
        return notice;
    }

    private HotelOrder order(
            String id,
            Long roomTypeId,
            String roomTypeName,
            String guestName,
            String guestPhone,
            Integer stayNights,
            String totalAmount,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            String status,
            String roomNo) {
        HotelOrder order = new HotelOrder();
        order.setId(id);
        order.setWxOpenid("dev-local-device");
        order.setRoomTypeId(roomTypeId);
        order.setRoomTypeName(roomTypeName);
        order.setUserPhone(guestPhone);
        order.setGuestName(guestName);
        order.setGuestPhone(guestPhone);
        order.setStayNights(stayNights);
        order.setTotalAmount(new BigDecimal(totalAmount));
        order.setCheckInDate(checkInDate);
        order.setCheckOutDate(checkOutDate);
        order.setStatus(status);
        order.setRoomNo(roomNo);
        order.setCreatedAt(LocalDateTime.now().minusHours(2));
        if ("staying".equals(status)) {
            order.setCheckInAt(LocalDateTime.now().minusHours(1));
        }
        if ("finished".equals(status)) {
            order.setCheckInAt(LocalDateTime.now().minusDays(1));
            order.setCheckOutAt(LocalDateTime.now().minusHours(3));
        }
        return order;
    }

    private void upsertUser(UserRepository userRepository, String username, String password, String role, String displayName) {
        User user = userRepository.findByUsername(username).orElseGet(User::new);
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        user.setDisplayName(displayName);
        user.setEnabled(true);
        userRepository.save(user);
    }
}
