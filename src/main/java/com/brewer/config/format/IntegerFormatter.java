package com.brewer.config.format;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class IntegerFormatter extends NumberFormatter<Integer>{

    @Autowired
    private Environment env;

    @Override
    protected String pattern(Locale locale) {
        return env.getProperty("integer.format", "#,##0");
    }
}
