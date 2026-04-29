package com.campus.market.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 全局配置
 * <p>目的：统一 {@link LocalDateTime} 的序列化格式为 "yyyy-MM-dd HH:mm:ss"，
 * 避免默认的 ISO-8601（含 T 和纳秒）对前端不友好。</p>
 */
@Configuration
public class JacksonConfig {

    public static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FMT     = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> {
            SimpleModule m = new SimpleModule();
            m.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATETIME_FMT));
            m.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATETIME_FMT));
            m.addSerializer(LocalDate.class, new LocalDateSerializer(DATE_FMT));
            m.addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE_FMT));
            m.addSerializer(LocalTime.class, new LocalTimeSerializer(TIME_FMT));
            m.addDeserializer(LocalTime.class, new LocalTimeDeserializer(TIME_FMT));
            builder.modulesToInstall(m);
        };
    }
}
