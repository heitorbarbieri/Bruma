/*=========================================================================

    Copyright © 2011 BIREME/PAHO/WHO

    This file is part of Bruma.

    Bruma is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as 
    published by the Free Software Foundation, either version 3 of 
    the License, or (at your option) any later version.

    Bruma is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public 
    License along with Bruma. If not, see <http://www.gnu.org/licenses/>.

=========================================================================*/

package bruma.utils;

import java.util.GregorianCalendar;

/**
 * 
 * @author Heitor Barbieri
 */
public class TimeString {
    private static final int YEAR = 31104000;
    private static final int MONTH = 2592000;
    private static final int WEEK = 604800;
    private static final int DAY = 86400;
    private static final int HOUR = 3600;
    private static final int MINUTE = 60;

    long beginTime = 0;

    public void start() {
        beginTime = new GregorianCalendar().getTimeInMillis();
    }

    public String getTime() {
        if (beginTime == 0) {
            throw new IllegalArgumentException(
                                       "call to begin() function is required");
        }

        final StringBuilder builder = new StringBuilder();
        final long totalTimeMili =
                        new GregorianCalendar().getTimeInMillis() - beginTime;
        long totalTime = totalTimeMili / 1000;
        final long miliseconds = totalTimeMili % 1000;
        final long years;
        final long months;
        final long weeks;
        final long days;
        final long hours;
        final long minutes;
        final long seconds;

        years = totalTime / YEAR;
        totalTime %= YEAR;

        months = totalTime / MONTH;
        totalTime %= MONTH;

        weeks = totalTime / WEEK;
        totalTime %= WEEK;

        days = totalTime / DAY;
        totalTime %= DAY;

        hours = totalTime / HOUR;
        totalTime %= HOUR;

        minutes = totalTime / MINUTE;
        totalTime %= MINUTE;

        seconds = totalTime;

        if (years > 0) {
            builder.append(years);
            builder.append(" year(s) ");
        }
        if (months > 0) {
            builder.append(months);
            builder.append(" month(s) ");
        }
        if (weeks > 0) {
            builder.append(weeks);
            builder.append(" week(s) ");
        }
        if (days > 0) {
            builder.append(days);
            builder.append(" day(s) ");
        }
        if (hours > 0) {
            builder.append(hours);
            builder.append(" hour(s) ");
        }
        if (minutes > 0) {
            builder.append(minutes);
            builder.append(" minute(s) ");
        }
        if (seconds > 0) {
            builder.append(seconds);
            builder.append(" second(s) ");
        }
        if (miliseconds > 0) {
            builder.append(miliseconds);
            builder.append(" milisecond(s) ");
        }
        if (builder.length() == 0) {
            builder.append("0 milisecond(s) ");
        }
        return builder.toString().trim();
    }
}
