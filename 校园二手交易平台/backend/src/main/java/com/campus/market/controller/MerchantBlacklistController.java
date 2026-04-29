package com.campus.market.controller;

import com.campus.market.annotation.RequiresRole;
import com.campus.market.common.PageResult;
import com.campus.market.common.Result;
import com.campus.market.common.UserContext;
import com.campus.market.common.enums.UserRole;
import com.campus.market.dto.BlacklistAddDTO;
import com.campus.market.service.BlacklistService;
import com.campus.market.vo.UserBlacklistVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/merchant/blacklist")
@RequiredArgsConstructor
@RequiresRole(UserRole.MERCHANT)
public class MerchantBlacklistController {

    private final BlacklistService blacklistService;

    @GetMapping
    public Result<PageResult<UserBlacklistVO>> list(
            @RequestParam(defaultValue = "1")  long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(blacklistService.listByMerchant(UserContext.getUserId(), pageNum, pageSize));
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody BlacklistAddDTO dto) {
        blacklistService.addByMerchant(UserContext.getUserId(), dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        blacklistService.removeByMerchant(UserContext.getUserId(), id);
        return Result.success();
    }
}
