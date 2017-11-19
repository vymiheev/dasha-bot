package ru.dasha.koshka.myav;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;

public class MySqlDbConnection {
    private static final Logger logger = LogManager.getLogger(MySqlDbConnection.class.getName());
    private static BasicDataSource connectionPool;

    private static final String DB_HOST = "178.62.240.115";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "bot_database";
    private static final String DB_USERNAME = "ADMIN";
    private static final String DB_PASSWORD = "ADMIN";

    public MySqlDbConnection() throws URISyntaxException, SQLException {
        connectionPool = new BasicDataSource();
        connectionPool.setUrl(String.format("jdbc:mysql://%s:%s/%s?useSSL=false", DB_HOST, DB_PORT, DB_NAME));//?useSSL=false
        connectionPool.setUsername(DB_USERNAME);
        connectionPool.setPassword(DB_PASSWORD);

        connectionPool.setDriverClassName("com.mysql.cj.jdbc.Driver");
        //connectionPool.setMinIdle(10);//todo understand
        //connectionPool.setMaxIdle(100);
        connectionPool.setMaxTotal(10);
        connectionPool.setInitialSize(10);
        connectionPool.setMaxOpenPreparedStatements(100);
    }

    public static BasicDataSource getConnectionPool() {
        return connectionPool;
    }

    public static Connection getConnection() {
        try {
            return connectionPool.getConnection();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}
