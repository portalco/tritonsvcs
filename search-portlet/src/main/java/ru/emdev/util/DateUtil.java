package ru.emdev.util;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.portlet.PortletRequest;
import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.util.CalendarFactoryUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import ru.emdev.util.web.WebKeys;

/**
 * @author Alexey Melnikov
 * 
 */
public class DateUtil {

    public static final String DF_DD_MM_YYYY = "dd.MM.yyyy";

    public static Date newDate() {
        return new Date();
    }

    /**
     * Returns date from request.
     * 
     * @param request
     * @param prefix
     * @return
     */
    public static Date getDateFromRequest(PortletRequest request, String prefix) {

        return getDateFromRequest(PortalUtil.getHttpServletRequest(request), prefix);
    }

    /**
     * Returns date from request.
     * 
     * @param request
     * @param prefix
     * @return
     */
    public static Date getDateFromRequest(HttpServletRequest request, String prefix) {

        return getDateFromRequest(request, prefix, null);
    }

    /**
     * Returns date from request.
     * 
     * @param request
     * @param prefix
     * @param timeZone
     *            timezone object or null, if null timezone will be taken from
     *            {@link ThemeDisplay}
     * @return
     */
    public static Date getDateFromRequest(HttpServletRequest request, String prefix, TimeZone timeZone) {
        int month = ParamUtil.getInteger(request, prefix + "Month");
        int day = ParamUtil.getInteger(request, prefix + "Day");
        int year = ParamUtil.getInteger(request, prefix + "Year");

        Locale locale = request.getLocale();
        if (timeZone == null) {
            ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
            timeZone = themeDisplay.getTimeZone();
        }

        return getDate(month, day, year, locale, timeZone);

    }

    /**
     * @deprecated should be private, use instead of this another public methods
     */
    @Deprecated
    public static Date getDate(int month, int day, int year, Locale locale, TimeZone timeZone) {

        Calendar date = CalendarFactoryUtil.getCalendar(timeZone, locale);

        date.set(Calendar.MONTH, month);
        date.set(Calendar.DATE, day);
        date.set(Calendar.YEAR, year);
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        return date.getTime();
    }
}