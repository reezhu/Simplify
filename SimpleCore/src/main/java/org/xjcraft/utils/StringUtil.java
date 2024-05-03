package org.xjcraft.utils;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Ree on 2017/7/12.
 */
public class StringUtil {
    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static String join(Object[] o, String flag) {
        StringBuffer str_buff = new StringBuffer();

        for (int i = 0, len = o.length; i < len; i++) {
            str_buff.append(String.valueOf(o[i]));
            if (i < len - 1) str_buff.append(flag);
        }

        return str_buff.toString();
    }

    /**
     * 两个时间相差距离多少天多少小时多少分多少秒  <br>
     * 时间参数为 Unix时间戳
     *
     * @param time1 时间参数 1 格式：1407132214
     * @param time2 时间参数 2 格式：1407132214
     * @return String 返回值为：xx天xx小时xx分xx秒
     */
    public static String getTimeLag(Long time1, Long time2) {
        long day = 0;
        long hour = 0;
        long min = 0;
        long sec = 0;

        long diff;
        diff = time2 - time1;
        day = diff / (24 * 60 * 60 * 1000);
        hour = (diff / (60 * 60 * 1000) - day * 24);
        min = ((diff / (60 * 1000)) - day * 24 * 60 - hour * 60);
        sec = (diff / 1000 - day * 24 * 60 * 60 - hour * 60 * 60 - min * 60);

        if (hour == 0) {
            return min + "分" + sec + "秒";
        } else if (day == 0) {
            return hour + "小时" + min + "分" + sec + "秒";
        } else {
            return day + "天" + hour + "小时" + min + "分" + sec + "秒";
        }
    }

    public static Timestamp getTime(String arg, boolean isAdd) throws NumberFormatException {
        if (arg == "forever")
            return getForever();
        Calendar cal = Calendar.getInstance();
        Pattern p = Pattern.compile("\\d+[ymdhMs]");
        Matcher m = p.matcher(arg);
        int count = 0;
        while (m.find()) {
            String regex = m.group();
            switch (regex.substring(regex.length() - 1, regex.length())) {
                case "y":
                    cal.add(Calendar.YEAR, Integer.parseInt(regex.substring(0, regex.length() - 1)) * (isAdd ? 1 : -1));
                    count++;
                    break;
                case "m":
                    cal.add(Calendar.MONTH, Integer.parseInt(regex.substring(0, regex.length() - 1)) * (isAdd ? 1 : -1));
                    count++;
                    break;
                case "d":
                    cal.add(Calendar.DAY_OF_MONTH, Integer.parseInt(regex.substring(0, regex.length() - 1)) * (isAdd ? 1 : -1));
                    count++;
                    break;
                case "h":
                    cal.add(Calendar.HOUR, Integer.parseInt(regex.substring(0, regex.length() - 1)) * (isAdd ? 1 : -1));
                    count++;
                    break;
                case "M":
                    cal.add(Calendar.MINUTE, Integer.parseInt(regex.substring(0, regex.length() - 1)) * (isAdd ? 1 : -1));
                    count++;
                    break;
                case "s":
                    cal.add(Calendar.SECOND, Integer.parseInt(regex.substring(0, regex.length() - 1)) * (isAdd ? 1 : -1));
                    count++;
                    break;
                default:
                    throw new NumberFormatException();
            }

        }
        if (count == 0) {
            throw new NumberFormatException();
        }
        return new Timestamp(cal.getTimeInMillis());
    }

    public static Timestamp getForever() {
        return new Timestamp(8099, 0, 1, 0, 0, 0, 0);
    }

    public static Timestamp getNow() {
        return new Timestamp(System.currentTimeMillis());
    }


    public static List<String> applyPlaceHolder(List<String> strings, Map<String, String> map) {
        if (strings == null) return strings;
        ArrayList<String> list = new ArrayList<>();
        for (String s : strings) {
            list.add(applyPlaceHolder(s, map));
        }

        return list;
    }

    public static String applyPlaceHolder(String s, Map<String, String> map) {
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                s = s.replaceAll("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return s.replaceAll("&", "§");
    }

    public static String parseCNTime(int seconds) {
        int d = seconds / 60 / 60 / 24;
        int h = seconds / 60 / 60 % 24;
        int m = seconds / 60 % 60;
        int s = seconds % 60;
        StringBuffer sb = new StringBuffer();
        if (d > 0) sb.append(d + "天");
        if (h > 0) sb.append(h + "时");
        if (m > 0) sb.append(m + "分");
        if (s > 0) sb.append(s + "秒");
        return sb.toString();
    }

    /**
     * 是否返回的是
     *
     * @param arg
     * @return
     */
    public static Boolean isYes(String arg) {
        if (arg == null) return false;
        return arg.equalsIgnoreCase("true") ||
                arg.equalsIgnoreCase("yes") ||
                arg.equalsIgnoreCase("y") ||
                arg.equalsIgnoreCase("1") ||
                arg.equalsIgnoreCase("t") ||
                arg.equalsIgnoreCase("ok");
    }
}
