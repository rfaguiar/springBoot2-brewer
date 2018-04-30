package com.brewer.config.format;

import org.springframework.format.Formatter;

import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Locale;

public abstract class TemporalFormatter<T extends Temporal> implements Formatter<T> {

    @Override
    public String print(T temporal, Locale locale) {
        DateTimeFormatter formatter = getDateTimeFormmatter(locale);
        return formatter.format(temporal);
    }


    @Override
    public T parse(String text, Locale locale) throws ParseException {
        DateTimeFormatter formatter = getDateTimeFormmatter(locale);
        return  parser(text, formatter);
    }

    private DateTimeFormatter getDateTimeFormmatter(Locale locale) {
        return DateTimeFormatter.ofPattern(pattern(locale));
    }

    protected abstract String pattern(Locale locale);

    protected abstract  T parser(String text, DateTimeFormatter formatter);
}
