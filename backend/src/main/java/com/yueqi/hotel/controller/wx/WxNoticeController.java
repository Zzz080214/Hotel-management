package com.yueqi.hotel.controller.wx;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yueqi.hotel.common.ApiResponse;
import com.yueqi.hotel.entity.Notice;
import com.yueqi.hotel.service.NoticeService;

@RestController
@RequestMapping("/api/wx/notices")
public class WxNoticeController {

    private final NoticeService noticeService;

    public WxNoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public ApiResponse<List<Notice>> list() {
        return ApiResponse.success(noticeService.listPublished());
    }
}
