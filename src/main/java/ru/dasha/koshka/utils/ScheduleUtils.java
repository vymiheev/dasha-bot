package ru.dasha.koshka.utils;

import ru.dasha.koshka.model.ActivityType;
import ru.dasha.koshka.model.FestActivity;
import ru.dasha.koshka.model.FestCourse;
import ru.dasha.koshka.model.FestSchedule;
import ru.dasha.koshka.myav.SpreadsheetConnection;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Daria on 08.11.2017.
 */
public class ScheduleUtils {
    private static final Pattern timePattern = Pattern.compile("(\\d*:\\d*)-(\\d*:\\d*)");
    private static final Pattern agePatternRange = Pattern.compile("(\\d*)-(\\d*)");
    private static final Pattern agePatternSinglePlus = Pattern.compile("(\\d*)\\+");

    private static final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
    private static final int SECTION_SIZE = 5;

    public static FestSchedule createSchedule() throws IOException {
        String range = "'Расписание лекций'";
        List<List<Object>> values = SpreadsheetConnection.getDataRange(range).getValues();
        if (values == null || values.size() == 0) {
            System.out.println("No data found.");
            return null;
        }
        int sectionsNum = values.get(0).size() / SECTION_SIZE + (values.get(0).size() % SECTION_SIZE == 0 ? 0 : 1);
        List<FestActivity> activities = new ArrayList<>();
        for (int i = 0; i < sectionsNum; i++) {
            activities.addAll(createSectionActivities(values, i, i == (sectionsNum - 1)));
        }
        return new FestSchedule(activities);
    }

    public static FestSchedule createGameSchedule() throws IOException {
        String range = "'Активности - весь день'";
        List<List<Object>> values = SpreadsheetConnection.getDataRange(range).getValues();
        if (values == null || values.size() == 0) {
            System.out.println("No data found.");
            return null;
        }
        List<FestActivity> activities = new ArrayList<>();
        for (int i = 1, activityId = 1000; i < values.size(); i++, activityId++) {
            activities.add(createGameActivity(activityId, values.get(i)));
        }
        return new FestSchedule(activities);
    }

    private static List<FestActivity> createSectionActivities(List<List<Object>> scheduleValues,
                                                              int sectionNum,
                                                              boolean isParentEvent) throws IOException {
        int offset = sectionNum * SECTION_SIZE;
        String name = (String) scheduleValues.get(0).get(offset);//todo regular expr () Pattern.compile("\\((.*?)\\)");
        List<FestActivity> activities = new ArrayList<>();
        //пропускаем первую строку - там заголовок
        int activityId = sectionNum * 20;
        for (int i = 3; i < scheduleValues.size(); i++, activityId++) {
            if (scheduleValues.get(i).get(offset) != null) {
                FestActivity activity = createActivity(scheduleValues.get(i), sectionNum, activityId, name, isParentEvent);
                if (activity.getName() != null && !activity.getName().isEmpty()) {
                    if ("ЗАКРЫТИЕ".toLowerCase().equals(activity.getName().toLowerCase())) {
                        FestActivity parentActivity = new FestActivity(
                                activity.getId(), activity.getStartTime(), activity.getEndTime(),
                                activity.getName(), activity.getTutor(), ActivityType.FOR_PARENTS, activity.getRoom(),
                                activity.getStartAge(), activity.getEndAge(), activity.getSecret());
                        activities.add(parentActivity);
                    }
                    activities.add(activity);
                }
            }
        }
        return activities;
    }

    private static FestActivity createActivity(List<Object> row,
                                               int sectionNum,
                                               int activityId,
                                               String room,
                                               boolean isParentEvent) {
        //todo room
        int offset = sectionNum * SECTION_SIZE;
        Date startDate = null, endDate = null;
        int startAge = 0, endAge = 18;
        String name = "", tutor = "", secret = "";
        List<Date> dates;
        int[] ages;
        switch (row.size() - offset) {
            case 5:
                if (!isParentEvent) {
                    secret = (String) row.get(offset + 4);
                }
            case 4:
                if (!isParentEvent) {
                    tutor = (String) row.get(offset + 3);
                } else {
                    secret = (String) row.get(offset + 3);
                }
            case 3:
                if (!isParentEvent) {
                    ages = parseAgeStr((String) row.get(offset + 2));
                    startAge = ages[0];
                    endAge = ages[1];
                } else {
                    tutor = (String) row.get(offset + 2);
                }
            case 2:
                name = (String) row.get(offset + 1);
            case 1:
                dates = parseDateStr((String) row.get(offset));
                startDate = dates.get(0);
                endDate = dates.get(1);
                break;
            default:
                secret = (String) row.get(offset + (isParentEvent ? 3 : 4));
                tutor = (String) row.get(offset + (isParentEvent ? 2 : 3));
                if (!isParentEvent) {
                    ages = parseAgeStr((String) row.get(offset + 2));
                    startAge = ages[0];
                    endAge = ages[1];
                }
                name = (String) row.get(offset + 1);
                dates = parseDateStr((String) row.get(offset));
                startDate = dates.get(0);
                endDate = dates.get(1);
                break;

        }
        return new FestActivity(activityId, startDate, endDate, name,
                tutor, isParentEvent ? ActivityType.FOR_PARENTS : ActivityType.LECTURE, room,
                startAge, endAge, secret);
    }

    private static List<Date> parseDateStr(String dateStr) {
        List<Date> result = new ArrayList<>();
        result.add(null);
        result.add(null);
        dateStr = dateStr.replaceAll("\\s", "");//удаляем все пробелы
        Matcher m = timePattern.matcher(dateStr);
        if (m.find()) {
            try {
                if (m.group(1) != null) {
                    result.set(0, timeFormatter.parse(m.group(1)));
                }
                if (m.group(2) != null) {
                    result.set(1, timeFormatter.parse(m.group(2)));
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private static int[] parseAgeStr(String dateStr) {
        int[] result = new int[2];
        result[0] = 0;
        result[1] = 100;
        dateStr = dateStr.replaceAll("\\s", "");//удаляем все пробелы
        Matcher m = agePatternRange.matcher(dateStr);
        if (m.find()) {
            if (m.group(1) != null) {
                result[0] = Integer.parseInt(m.group(1));
            }
            if (m.group(2) != null) {
                result[1] = Integer.parseInt(m.group(2));
            }
        } else {
            m = agePatternSinglePlus.matcher(dateStr);
            if (m.find()) {
                if (m.group(1) != null) {
                    result[0] = Integer.parseInt(m.group(1));
                }
                result[1] = 100;
            }
        }
        return result;
    }

    public static List<FestActivity> findSuitableActivities(List<FestActivity> allActivities, int age) {
        List<FestActivity> suitable;
        Date now = new Date();
        if (age != 50) {
            suitable = allActivities.stream().
                    filter(a -> a.getStartAge() <= age && a.getEndAge() >= age &&
                            ActivityType.LECTURE.equals(a.getType()) &&
                            isSuitableTime(a.getStartTime(), now)).
                    collect(Collectors.toList());
        } else {
            suitable = allActivities.stream().
                    filter(a -> ActivityType.FOR_PARENTS.equals(a.getType()) &&
                            isSuitableTime(a.getStartTime(), now)).
                    collect(Collectors.toList());
        }
        if (suitable.size() == 0) {
            return suitable;
        }
        suitable.sort((FestActivity a1, FestActivity a2) -> a1.getStartTime().compareTo(a2.getStartTime()));
        FestActivity earliestActivity = suitable.get(0);
        suitable = suitable.stream()
                .filter(a -> a.getStartTime().equals(earliestActivity.getStartTime()))
                .collect(Collectors.toList());
        return suitable;
    }

    public static boolean timeAfterNow(Date d1) {
        //d1 = parseDateStr("12:52 - 12:00").get(0);
        Calendar today = Calendar.getInstance();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(d1);
        calendar.set(Calendar.YEAR, today.get(Calendar.YEAR));
        calendar.set(Calendar.MONTH, today.get(Calendar.MONTH));
        calendar.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH));
        Date now = new Date();
        return calendar.getTime().after(now);
    }

    /*public static boolean isSuitableTime(Date startTime, Date now) {
        //d1 = parseDateStr("12:52 - 12:00").get(0);
        Calendar today = Calendar.getInstance();
        today.set(Calendar.AM_PM, Calendar.PM);
        today.setTime(now);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startTime);
        calendar.set(Calendar.YEAR, today.get(Calendar.YEAR));
        calendar.set(Calendar.MONTH, today.get(Calendar.MONTH));
        calendar.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH));
        calendar.add(Calendar.HOUR_OF_DAY, -1);
        Date a = calendar.getTime();
        calendar.set(Calendar.HOUR_OF_DAY, 1);
        calendar.add(Calendar.MINUTE, 30);
        Date b = calendar.getTime();


        today.set(Calendar.HOUR_OF_DAY, 12);
        today.set(Calendar.MINUTE, 0);
        now = today.getTime();

        return a.before(now) && b.after(now);
    }*/

    private static FestActivity createGameActivity(int activityId, List<Object> row) {
        int[] ages;
        int startAge = 0, endAge = 100;
        String name = "", room = "", secretWord = "";
        switch (row.size()) {
            case 4:
                secretWord = (String) row.get(3);
            case 3:
                ages = parseAgeStr((String) row.get(2));
                startAge = ages[0];
                endAge = ages[1];
            case 2:
                name = (String) row.get(1);
            case 1:
                room = (String) row.get(0);
                break;
            default:
                break;
        }
        return new FestActivity(activityId, null, null, name, null,
                ActivityType.GAME, room, startAge, endAge, secretWord);
    }

    /**
     * determines if time is suitable for showing in schedule
     *
     * @param activityTime
     * @return true if activityTime >= now-20min
     */
    private static boolean isSuitableTime(Date activityTime, Date now) {
        Calendar activityCalendar = Calendar.getInstance();
        activityCalendar.setTime(activityTime);
        Calendar today = Calendar.getInstance();
        activityCalendar.set(Calendar.YEAR, today.get(Calendar.YEAR));
        activityCalendar.set(Calendar.MONTH, today.get(Calendar.MONTH));
        activityCalendar.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH));
        activityCalendar.add(Calendar.MINUTE, 20);
        Date aTime = activityCalendar.getTime();
        return now.equals(aTime) || aTime.after(now);
    }

    public static List<FestCourse> createCourses() throws IOException {
        String range = "'Курсы'";
        List<List<Object>> values = SpreadsheetConnection.getDataRange(range).getValues();
        if (values == null || values.size() == 0) {
            System.out.println("No fest courses data found.");
            return null;
        }
        List<FestCourse> courses = new ArrayList<>();
        for (int i = 1; i < values.size(); i++) {
            courses.add(createCourse(values.get(i)));
        }
        return courses;
    }

    private static FestCourse createCourse(List<Object> row) {
        int[] ages;
        int startAge = 0, endAge = 100;
        String name = "", descr = "";
        switch (row.size()) {
            case 3:
                descr = (String) row.get(2);
            case 2:
                name = (String) row.get(1);
            case 1:
                ages = parseAgeStr((String) row.get(0));
                startAge = ages[0];
                endAge = ages[1];
                break;
            default:
                break;
        }
        return new FestCourse(startAge, endAge, name, descr);

    }

}
