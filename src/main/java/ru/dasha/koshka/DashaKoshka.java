package ru.dasha.koshka;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private static final Logger logger = LogManager.getLogger(DashaKoshka.class.getName());

    public static void main(String[] args) {
        ApiContextInitializer.init();
        TelegramBotsApi botsApi = new TelegramBotsApi();
        try {
            logger.info("Init bot...");
            botsApi.registerBot(new DigitalFestBot());
            logger.info("Bot registered.");
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
        try {
            logger.info("Init db connection...");
            new MySqlDbConnection();
            logger.info("DB connection acquired!.");
        } catch (URISyntaxException | SQLException e) {
            logger.error(e.getMessage(), e);
        }
    }
}

