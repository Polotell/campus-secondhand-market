package com.campus.market.service;

import com.campus.market.common.PageResult;
import com.campus.market.dto.CarouselSaveDTO;
import com.campus.market.vo.CarouselVO;

import java.util.List;

public interface CarouselService {

    List<CarouselVO> listActivePublic();

    PageResult<CarouselVO> listForAdmin(long pageNum, long pageSize);

    Long create(CarouselSaveDTO dto);

    void update(Long id, CarouselSaveDTO dto);

    void delete(Long id);
}
