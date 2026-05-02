package com.yueqi.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yueqi.hotel.entity.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}
