package com.campus.market.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.campus.market.common.Result;
import com.campus.market.common.ResultCode;
import com.campus.market.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * 通用文件上传
 * <p>
 * 业务场景：
 * <ul>
 *   <li>用户头像</li>
 *   <li>商家营业执照、身份证</li>
 *   <li>商品图片（可多次调用）</li>
 *   <li>评价晒图、退货凭证</li>
 * </ul>
 * <p>
 * 实现说明：
 * <ul>
 *   <li>存储目录来自 {@code file.upload-dir}，已在 {@code WebMvcConfig} 映射为 /uploads/** 静态资源。</li>
 *   <li>按日期分子目录避免单目录文件过多（yyyy-MM-dd）。</li>
 *   <li>返回的 {@code url} 是**相对路径**，前端拼接后通过 /api/uploads/xxx 访问。</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.access-prefix}")
    private String accessPrefix;

    /** 限制上传的图片类型（简单白名单） */
    private static final String[] ALLOWED_EXT = {"jpg", "jpeg", "png", "gif", "bmp", "webp"};

    /**
     * 图片上传。
     * <p>本接口允许匿名访问（放在白名单），因为"商家注册"场景需要先上传营业执照/身份证再提交表单。
     * 已通过以下措施控制风险：</p>
     * <ul>
     *   <li>后缀白名单（jpg/png/gif/webp 等）+ 大小限制（application.yml 里 spring.servlet.multipart.max-file-size）</li>
     *   <li>文件名用 UUID 重命名 + 按日期子目录，防路径穿越</li>
     *   <li>返回相对 URL，底层仅做静态资源映射，执行权限不会被触发</li>
     * </ul>
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw BusinessException.of(ResultCode.BAD_REQUEST, "请选择文件");
        }

        String originName = file.getOriginalFilename();
        String ext = FileUtil.extName(originName);
        if (StrUtil.isBlank(ext) || !isAllowed(ext)) {
            throw BusinessException.of(ResultCode.BAD_REQUEST, "只支持图片格式：jpg/png/gif/webp");
        }

        String datePath = LocalDate.now().toString();
        String newName = UUID.randomUUID().toString().replace("-", "") + "." + ext.toLowerCase();
        File dir = new File(uploadDir + datePath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw BusinessException.of(ResultCode.INTERNAL_ERROR, "创建上传目录失败");
        }

        File dest = new File(dir, newName);
        file.transferTo(dest);

        // 前端访问时拼接：http://<host>/api + url
        String url = accessPrefix + datePath + "/" + newName;
        log.info("文件上传成功 {} -> {}", originName, dest.getAbsolutePath());
        return Result.success(Map.of("url", url));
    }

    private static boolean isAllowed(String ext) {
        for (String a : ALLOWED_EXT) {
            if (a.equalsIgnoreCase(ext)) return true;
        }
        return false;
    }
}
