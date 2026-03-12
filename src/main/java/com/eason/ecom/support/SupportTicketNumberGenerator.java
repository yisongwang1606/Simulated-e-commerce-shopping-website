package com.eason.ecom.support;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

@Component
public class SupportTicketNumberGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public String next() {
        return "TKT-" + FORMATTER.format(LocalDateTime.now())
                + "-"
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }
}
