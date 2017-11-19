package ru.dasha.koshka.myav;

import java.util.Calendar;
import java.util.Timer;

/**
 * Created by Daria on 18.11.2017.
 */
public class TimeWorker {
    private Timer timer;
    private DigitalFestBot.MyTimerTask timerTask;

    public TimeWorker(DigitalFestBot.MyTimerTask timerTask) {
        this.timer = new Timer();
        this.timerTask = timerTask;
    }

    public void start() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 20);
        calendar.set(Calendar.HOUR_OF_DAY, 11);
        calendar.set(Calendar.MINUTE, 29);
        calendar.set(Calendar.SECOND, 0);
        timer.schedule(timerTask, calendar.getTime());
    }

}