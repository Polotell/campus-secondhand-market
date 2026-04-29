package com.campus.market.controller;

import com.campus.market.common.Result;
import com.campus.market.service.CarouselService;
import com.campus.market.vo.CarouselVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页公开数据（游客可访问，见 {@code WebMvcConfig} 白名单 /home/**）
 */
@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {

    private final CarouselService carouselService;

    @GetMapping("/carousels")
    public Result<List<CarouselVO>> carousels() {
        return Result.success(carouselService.listActivePublic());
    }
}
