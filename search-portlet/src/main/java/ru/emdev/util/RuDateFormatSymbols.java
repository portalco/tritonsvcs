package ru.emdev.util;

import java.text.DateFormatSymbols;

/**
 * special date formats symbols to support correct month names in Russian
 * 
 * @author akakunin
 * 
 */
public class RuDateFormatSymbols extends DateFormatSymbols {

    private static final long serialVersionUID = 4526243676562545848L;

    @Override
    public String[] getMonths() {
        return new String[] {"января", "февраля", "марта", "апреля", "мая", "июня",
                "июля", "августа", "сентября", "октября", "ноября", "декабря"};
    }

}
