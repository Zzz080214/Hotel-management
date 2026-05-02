package com.yueqi.hotel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yueqi.hotel.entity.RoomType;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    List<RoomType> findByEnabledTrueOrderByPriceAsc();
}
