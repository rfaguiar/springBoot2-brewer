package com.brewer.config.format;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

@Component
public class BigDecimalFormatter extends NumberFormatter<BigDecimal>{

    @Autowired
    private Environment env;

    @Override
    protected String pattern(Locale locale) {
        return env.getProperty("bigdecimal.format", "#,##0.00");
    }
}
