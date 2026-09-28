package com.example.fasse_back.support.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SeedGuardTest {

    @Test
    void isConfirmed_onlyWithYes() {
        assertThat(SeedGuard.isConfirmed(new String[] { "yes" })).isTrue();
        assertThat(SeedGuard.isConfirmed(new String[] { "" })).isFalse();
        assertThat(SeedGuard.isConfirmed(new String[] { "YES" })).isFalse();
        assertThat(SeedGuard.isConfirmed(new String[] { "true" })).isFalse();
        assertThat(SeedGuard.isConfirmed(new String[0])).isFalse();
        assertThat(SeedGuard.isConfirmed(null)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "jdbc:mysql://localhost:3306/fasse?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true, localhost, fasse",
            "jdbc:mysql://localhost/fasse, localhost, fasse",
            "jdbc:mysql://LOCALHOST:3306/fasse, LOCALHOST, fasse",
            "jdbc:mysql://127.0.0.1:3306/fasse_dev, 127.0.0.1, fasse_dev",
            "jdbc:mysql://[::1]:3306/fasse, ::1, fasse",
    })
    void localTarget_acceptsLocalHosts(String url, String host, String database) {
        assertThat(SeedGuard.localTarget(url)).contains(new SeedGuard.Target(host, database));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:mysql://fasse-stg.cluster-xxxx.ap-northeast-1.rds.amazonaws.com:3306/fasse",
            "jdbc:mysql://192.168.0.10:3306/fasse",
            "jdbc:mysql://localhost.example.com:3306/fasse",
            "jdbc:mysql://localhost,remote-host:3306/fasse",
            "jdbc:mysql://[2001:db8::1]:3306/fasse",
    })
    void localTarget_rejectsRemoteHosts(String url) {
        assertThat(SeedGuard.localTarget(url)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "jdbc:postgresql://localhost:5432/fasse",
            "jdbc:mysql:loadbalance://localhost:3306/fasse",
            "jdbc:mysql://localhost:3306",
            "localhost:3306/fasse",
    })
    void localTarget_rejectsUnparsableUrls(String url) {
        assertThat(SeedGuard.localTarget(url)).isEmpty();
    }

    @Test
    void parse_extractsHostAndDatabaseWithoutParameters() {
        assertThat(SeedGuard.parse("jdbc:mysql://db.example.com:3306/fasse?user=admin&password=secret"))
                .contains(new SeedGuard.Target("db.example.com", "fasse"));
    }
}
