package com.brewer.config.format;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class LocalDateFormatter extends TemporalFormatter<LocalDate>{

    @Autowired
    private Environment env;

    @Override
    protected String pattern(Locale locale) {
        return env.getProperty("localdate.format-" + locale, "dd/MMM/yyyy");
    }

    @Override
    protected LocalDate parser(String text, DateTimeFormatter formatter) {
        return LocalDate.parse(text, formatter);
    }
}
