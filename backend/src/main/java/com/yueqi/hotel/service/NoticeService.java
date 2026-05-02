package com.yueqi.hotel.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yueqi.hotel.dto.NoticeRequest;
import com.yueqi.hotel.entity.Notice;
import com.yueqi.hotel.repository.NoticeRepository;

@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    @Transactional(readOnly = true)
    public List<Notice> listPublished() {
        return noticeRepository.findByPublishedTrueOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public List<Notice> listAll() {
        return noticeRepository.findAll();
    }

    @Transactional
    public Notice create(NoticeRequest request) {
        Notice notice = new Notice();
        fill(notice, request);
        notice.setPublishDate(LocalDate.now());
        return noticeRepository.save(notice);
    }

    @Transactional
    public Notice update(Long id, NoticeRequest request) {
        Notice notice = getRequired(id);
        fill(notice, request);
        if (notice.getPublishDate() == null) {
            notice.setPublishDate(LocalDate.now());
        }
        return noticeRepository.save(notice);
    }

    @Transactional
    public void delete(Long id) {
        noticeRepository.delete(getRequired(id));
    }

    @Transactional(readOnly = true)
    public Notice getRequired(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "公告不存在"));
    }

    private void fill(Notice notice, NoticeRequest request) {
        notice.setTitle(request.title());
        notice.setLevel(request.level());
        notice.setContent(request.content());
        notice.setPublished(request.published() == null || request.published());
    }
}
