package com.brewer.config.format;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class LocalDateTimeFormatter extends TemporalFormatter<LocalDateTime>{

    @Autowired
    private Environment env;

    @Override
    protected String pattern(Locale locale) {
        return env.getProperty("localdatetime.format-" + locale, "dd/MMM/yyyy HH:mm");
    }

    @Override
    protected LocalDateTime parser(String text, DateTimeFormatter formatter) {
        return LocalDateTime.parse(text, formatter);
    }
}
