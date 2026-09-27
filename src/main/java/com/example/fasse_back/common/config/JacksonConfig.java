package com.example.fasse_back.common.config;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.LogicalType;

/**
 * JSON の入出力設定(design.md 5 節・6.1 節)。
 *
 * <ul>
 * <li>項目名はスネークケース、スキーマに無い項目は無視する</li>
 * <li>暗黙の型変換(文字列→数値、数値→文字列、小数→整数、数値→ENUM 等)を行わない</li>
 * <li>{@code created_at} / {@code updated_at}({@link Instant})は UTC のミリ秒付き、
 * {@code sales_datetime}({@link OffsetDateTime})はオフセット付きの秒精度で出力する</li>
 * </ul>
 */
@Configuration
public class JacksonConfig {

    /** created_at / updated_at の出力形式(例: 2026-07-12T10:30:00.000Z) */
    static final DateTimeFormatter INSTANT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    /** sales_datetime の出力形式(例: 2026-07-12T19:30:00+09:00) */
    static final DateTimeFormatter OFFSET_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    @Bean
    Jackson2ObjectMapperBuilderCustomizer fasseJacksonCustomizer() {
        return builder -> builder
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .featuresToDisable(
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        DeserializationFeature.ACCEPT_FLOAT_AS_INT,
                        MapperFeature.ALLOW_COERCION_OF_SCALARS,
                        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .featuresToEnable(
                        DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS,
                        JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
                .serializerByType(Instant.class, new FormattedSerializer<>(Instant.class, INSTANT_FORMAT))
                .serializerByType(OffsetDateTime.class,
                        new FormattedSerializer<>(OffsetDateTime.class, OFFSET_DATE_TIME_FORMAT))
                .postConfigurer(mapper -> {
                    // 数値・真偽値を文字列項目として受け付けない
                    mapper.coercionConfigFor(LogicalType.Textual)
                            .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                            .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                            .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
                    // 空文字を null や既定値に変換しない
                    mapper.coercionConfigDefaults()
                            .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail);
                    // 日付に時刻付きの値や数値を受け付けない
                    mapper.configOverride(LocalDate.class)
                            .setFormat(JsonFormat.Value.forLeniency(false));
                });
    }

    /** {@link DateTimeFormatter} で文字列に変換して出力するシリアライザ */
    private static final class FormattedSerializer<T extends java.time.temporal.TemporalAccessor>
            extends StdSerializer<T> {

        private final transient DateTimeFormatter formatter;

        FormattedSerializer(Class<T> type, DateTimeFormatter formatter) {
            super(type);
            this.formatter = formatter;
        }

        @Override
        public void serialize(T value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(formatter.format(value));
        }
    }
}
