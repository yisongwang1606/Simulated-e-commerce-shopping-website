package com.eason.ecom.support;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

@Component
public class ShipmentNumberGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public String next() {
        return "SHP-" + LocalDateTime.now().format(FORMATTER) + "-"
                + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
