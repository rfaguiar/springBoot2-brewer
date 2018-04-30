package com.brewer.config.format;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class LocalTimeFormatter extends TemporalFormatter<LocalTime>{

    @Autowired
    private Environment env;

    @Override
    protected String pattern(Locale locale) {
        return env.getProperty("localtime.format-" + locale, "HH:mm");
    }

    @Override
    protected LocalTime parser(String text, DateTimeFormatter formatter) {
        return LocalTime.parse(text, formatter);
    }
}
