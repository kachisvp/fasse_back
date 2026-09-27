package com.example.fasse_back.common.slipno;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.fasse_back.support.MapperTest;
import com.example.fasse_back.support.TestDataLoader;

@MapperTest
class SlipNoMapperTest {

    @Autowired
    SlipNoMapper slipNoMapper;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        TestDataLoader.load(jdbc);
    }

    private long next(String counterName) {
        slipNoMapper.increment(counterName);
        return slipNoMapper.selectLastInsertId();
    }

    @Test
    void increment_existingCounter_continuesSequence() {
        // テストデータの purchase_no#2026-07-01 は 2
        assertThat(next("purchase_no#2026-07-01")).isEqualTo(3L);
        assertThat(next("purchase_no#2026-07-01")).isEqualTo(4L);
        assertThat(jdbc.queryForObject(
                "SELECT value FROM t_slip_no_counter WHERE counter_name = 'purchase_no#2026-07-01'", Long.class))
                .isEqualTo(4L);
    }

    @Test
    void increment_newDate_startsFromOne() {
        assertThat(next("purchase_no#2026-08-01")).isEqualTo(1L);
        assertThat(next("purchase_no#2026-08-01")).isEqualTo(2L);
        assertThat(next("purchase_no#2026-08-02")).isEqualTo(1L);
    }

    @Test
    void increment_countersArePerSlipType() {
        assertThat(next("sales_no#2026-07-01")).isEqualTo(4L);
        assertThat(next("purchase_no#2026-07-02")).isEqualTo(2L);
    }
}
