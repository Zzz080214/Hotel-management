package com.yueqi.hotel.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yueqi.hotel.entity.Notice;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findByPublishedTrueOrderByIdDesc();
}
