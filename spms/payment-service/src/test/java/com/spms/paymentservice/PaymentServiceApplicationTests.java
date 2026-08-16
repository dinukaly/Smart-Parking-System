package com.spms.paymentservice;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentServiceApplicationTests {

    @Test
    void applicationClassLoads() {
        assertThat(PaymentServiceApplication.class).isNotNull();
    }

}
