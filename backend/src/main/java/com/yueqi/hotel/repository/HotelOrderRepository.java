package com.yueqi.hotel.repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.yueqi.hotel.entity.HotelOrder;

public interface HotelOrderRepository extends JpaRepository<HotelOrder, String> {

    List<HotelOrder> findByStatusOrderByCreatedAtDesc(String status);

    List<HotelOrder> findByUserPhoneOrderByCreatedAtDesc(String userPhone);

    List<HotelOrder> findByUserPhoneAndStatusOrderByCreatedAtDesc(String userPhone, String status);

    List<HotelOrder> findByWxOpenidOrderByCreatedAtDesc(String wxOpenid);

    List<HotelOrder> findByWxOpenidAndStatusOrderByCreatedAtDesc(String wxOpenid, String status);

    default List<HotelOrder> findAllNewestFirst() {
        return findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    long countByStatus(String status);

    default long countToday() {
        String todayPrefix = "HT" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return findAll().stream()
                .filter(o -> o.getId() != null && o.getId().startsWith(todayPrefix))
                .count();
    }
}
