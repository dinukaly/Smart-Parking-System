package com.spms.parkingservice;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParkingServiceApplicationTests {

    @Test
    void mainApplicationClassLoads() {
        assertThat(ParkingServiceApplication.class).isNotNull();
    }
}
