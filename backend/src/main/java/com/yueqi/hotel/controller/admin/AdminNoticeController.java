package com.yueqi.hotel.controller.admin;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yueqi.hotel.common.ApiResponse;
import com.yueqi.hotel.dto.NoticeRequest;
import com.yueqi.hotel.entity.Notice;
import com.yueqi.hotel.service.NoticeService;

@RestController
@RequestMapping("/api/admin/notices")
public class AdminNoticeController {

    private final NoticeService noticeService;

    public AdminNoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public ApiResponse<List<Notice>> list() {
        return ApiResponse.success(noticeService.listAll());
    }

    @PostMapping
    public ApiResponse<Notice> create(@Valid @RequestBody NoticeRequest request) {
        return ApiResponse.success(noticeService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Notice> update(@PathVariable Long id, @Valid @RequestBody NoticeRequest request) {
        return ApiResponse.success(noticeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return ApiResponse.success();
    }
}
