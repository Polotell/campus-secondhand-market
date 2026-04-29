package com.campus.market.controller;

import com.campus.market.annotation.RequiresRole;
import com.campus.market.common.PageResult;
import com.campus.market.common.Result;
import com.campus.market.common.enums.UserRole;
import com.campus.market.dto.CarouselSaveDTO;
import com.campus.market.service.CarouselService;
import com.campus.market.vo.CarouselVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/admin/carousels")
@RequiredArgsConstructor
@RequiresRole(UserRole.ADMIN)
public class AdminCarouselController {

    private final CarouselService carouselService;

    @GetMapping
    public Result<PageResult<CarouselVO>> list(
            @RequestParam(defaultValue = "1")  long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(carouselService.listForAdmin(pageNum, pageSize));
    }

    @PostMapping
    public Result<Map<String, Long>> create(@Valid @RequestBody CarouselSaveDTO dto) {
        Long id = carouselService.create(dto);
        return Result.success(Map.of("id", id));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CarouselSaveDTO dto) {
        carouselService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        carouselService.delete(id);
        return Result.success();
    }
}
