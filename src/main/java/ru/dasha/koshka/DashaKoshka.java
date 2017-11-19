package ru.dasha.koshka;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import ru.dasha.koshka.myav.DigitalFestBot;
import ru.dasha.koshka.myav.MySqlDbConnection;

import java.net.URISyntaxException;
import java.sql.SQLException;

/**
 * Created by Daria on 07.11.2017.
 */
public class DashaKoshka {
    public static void main(String[] args) {

        ApiContextInitializer.init();
        TelegramBotsApi botsApi = new TelegramBotsApi();
        try {
            botsApi.registerBot(new DigitalFestBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        try {
            new MySqlDbConnection();
        } catch (URISyntaxException | SQLException e) {
            e.printStackTrace();
        }
    }
}

