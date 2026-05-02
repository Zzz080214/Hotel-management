package com.yueqi.hotel.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yueqi.hotel.dto.RoomTypeRequest;
import com.yueqi.hotel.entity.RoomType;
import com.yueqi.hotel.repository.RoomTypeRepository;

@Service
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;

    public RoomTypeService(RoomTypeRepository roomTypeRepository) {
        this.roomTypeRepository = roomTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<RoomType> listEnabled() {
        return roomTypeRepository.findByEnabledTrueOrderByPriceAsc();
    }

    @Transactional(readOnly = true)
    public List<RoomType> recommend(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 10));
        return listEnabled().stream()
                .sorted(Comparator.comparing((RoomType room) -> "hot".equals(room.getStatus()) ? 0 : 1)
                        .thenComparing(RoomType::getPrice))
                .limit(safeLimit)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomType getEnabled(Long id) {
        RoomType roomType = getRequired(id);
        if (!Boolean.TRUE.equals(roomType.getEnabled())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "房型不存在或已下架");
        }
        return roomType;
    }

    @Transactional(readOnly = true)
    public List<RoomType> listAll() {
        return roomTypeRepository.findAll();
    }

    @Transactional
    public RoomType create(RoomTypeRequest request) {
        RoomType roomType = new RoomType();
        roomType.setEnabled(true);
        fill(roomType, request);
        return roomTypeRepository.save(roomType);
    }

    @Transactional
    public RoomType update(Long id, RoomTypeRequest request) {
        RoomType roomType = getRequired(id);
        fill(roomType, request);
        return roomTypeRepository.save(roomType);
    }

    @Transactional
    public void disable(Long id) {
        RoomType roomType = getRequired(id);
        roomType.setEnabled(false);
        roomTypeRepository.save(roomType);
    }

    @Transactional(readOnly = true)
    public RoomType getRequired(Long id) {
        return roomTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "房型不存在"));
    }

    private void fill(RoomType roomType, RoomTypeRequest request) {
        roomType.setName(request.name());
        roomType.setPrice(request.price());
        roomType.setArea(request.area());
        roomType.setBed(request.bed());
        roomType.setBreakfast(request.breakfast() == null ? "无" : request.breakfast());
        roomType.setOccupancy(request.occupancy() == null ? "2人" : request.occupancy());
        roomType.setStatus(request.status());
        roomType.setTag(request.tag());
        roomType.setImage(request.image());
        roomType.setSummary(request.summary());
        roomType.setTotalRooms(request.totalRooms());
        roomType.setAvailableRooms(Math.min(request.availableRooms(), request.totalRooms()));
        roomType.setFeatures(request.features());
    }
}
