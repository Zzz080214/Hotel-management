package com.yueqi.hotel.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yueqi.hotel.entity.RoomType;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    List<RoomType> findByEnabledTrueOrderByPriceAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RoomType r where r.id = :id")
    Optional<RoomType> findByIdForUpdate(@Param("id") Long id);
}
