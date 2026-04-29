package com.campus.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.PageResult;
import com.campus.market.common.ResultCode;
import com.campus.market.dto.CarouselSaveDTO;
import com.campus.market.entity.Carousel;
import com.campus.market.exception.BusinessException;
import com.campus.market.mapper.CarouselMapper;
import com.campus.market.service.CarouselService;
import com.campus.market.vo.CarouselVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarouselServiceImpl implements CarouselService {

    private final CarouselMapper carouselMapper;

    @Override
    public List<CarouselVO> listActivePublic() {
        List<Carousel> list = carouselMapper.selectList(new LambdaQueryWrapper<Carousel>()
                .eq(Carousel::getStatus, "ON")
                .orderByDesc(Carousel::getSort)
                .orderByDesc(Carousel::getId));
        return list.stream().map(this::toVo).collect(Collectors.toList());
    }

    @Override
    public PageResult<CarouselVO> listForAdmin(long pageNum, long pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 20;
        Page<Carousel> page = carouselMapper.selectPage(Page.of(pageNum, pageSize),
                new LambdaQueryWrapper<Carousel>().orderByDesc(Carousel::getSort).orderByDesc(Carousel::getId));
        List<CarouselVO> vos = page.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.of(page, vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CarouselSaveDTO dto) {
        Carousel c = new Carousel();
        c.setImageUrl(dto.getImageUrl());
        c.setLinkUrl(blankToNull(dto.getLinkUrl()));
        c.setSort(dto.getSort() == null ? 0 : dto.getSort());
        c.setStatus(normalizeStatus(dto.getStatus()));
        carouselMapper.insert(c);
        return c.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CarouselSaveDTO dto) {
        Carousel c = carouselMapper.selectById(id);
        if (c == null) throw BusinessException.of(ResultCode.NOT_FOUND, "轮播不存在");
        c.setImageUrl(dto.getImageUrl());
        c.setLinkUrl(blankToNull(dto.getLinkUrl()));
        c.setSort(dto.getSort() == null ? 0 : dto.getSort());
        c.setStatus(normalizeStatus(dto.getStatus()));
        carouselMapper.updateById(c);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (carouselMapper.selectById(id) == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "轮播不存在");
        }
        carouselMapper.deleteById(id);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String normalizeStatus(String s) {
        if (s != null && s.equalsIgnoreCase("OFF")) return "OFF";
        return "ON";
    }

    private CarouselVO toVo(Carousel c) {
        CarouselVO v = new CarouselVO();
        BeanUtils.copyProperties(c, v);
        return v;
    }
}
