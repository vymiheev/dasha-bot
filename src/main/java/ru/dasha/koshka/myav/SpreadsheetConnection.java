package ru.dasha.koshka.myav; /**
 * Created by Daria on 07.11.2017.
 */

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsRequestInitializer;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.dasha.koshka.DashaKoshka;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class SpreadsheetConnection {
    private static final Logger logger = LogManager.getLogger(SpreadsheetConnection.class.getName());
    private static final String APPLICATION_NAME = "Dasha Quickstart";
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/sheets.googleapis.com-java-quickstart");
    private static FileDataStoreFactory DATA_STORE_FACTORY;
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static HttpTransport HTTP_TRANSPORT;
    private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS_READONLY);
    private static final String API_KEY_FILE = "google-api-key.secret";
    private static final String SPREADSHEET_ID = "1gLfAK6RVwRqHdvFqKHBFEt5F6Zb4hIimt2CmOuMUC-8";//"1eC7zzj1L7zD4xycuwpAw-Kh5u8eG7nxtDGPvhlZGivM";

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            System.exit(1);
        }
    }

    /**
     * Build and return an authorized Sheets API client service.
     *
     * @return an authorized Sheets API client service
     * @throws IOException
     */
    public static Sheets getSheetsService() throws IOException, URISyntaxException {
/*        logger.debug(DashaKoshka.class.getResource(".").toString());
        byte[] bytes = new byte[0];
        try {
            bytes = Files.readAllBytes(Paths.get(DashaKoshka.class.getResource("../../../../conf/" + API_KEY_FILE).toURI()));
            logger.debug(bytes.length);
            logger.debug("success");
        } catch (Exception e) {
            try {
                bytes = Files.readAllBytes(Paths.get(DashaKoshka.class.getResource("../../../conf/" + API_KEY_FILE).toURI()));
                logger.debug(bytes.length);
                logger.debug("success");
            } catch (Exception e1) {
                try {
                    bytes = Files.readAllBytes(Paths.get(DashaKoshka.class.getResource("../../conf/" + API_KEY_FILE).toURI()));
                    logger.debug(bytes.length);
                    logger.debug("success");
                } catch (Exception e2) {
                    try {
                        bytes = Files.readAllBytes(Paths.get(DashaKoshka.class.getResource("../conf/" + API_KEY_FILE).toURI()));
                        logger.debug(bytes.length);
                        logger.debug("success");
                    } catch (Exception e3) {
                        try {
                            bytes = Files.readAllBytes(Paths.get(DashaKoshka.class.getResource("conf/" + API_KEY_FILE).toURI()));
                            logger.debug(bytes.length);
                            logger.debug("success");
                        } catch (Exception e4) {

                        }
                    }
                }
            }
        }

        logger.info(new String(bytes));*/
        byte[] bytes = Files.readAllBytes(Paths.get("conf" + File.separator + API_KEY_FILE));
        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, null)
                .setApplicationName(APPLICATION_NAME)
                .setSheetsRequestInitializer(new SheetsRequestInitializer(new String(bytes)))
                .build();
    }

    /*public FestSchedule createSchedule() throws IOException {
        String range = "Лист1!A:C";
        List<List<Object>> values = getDataRange(range).getValues();
        FestSchedule schedule = null;
        if (values == null || values.size() == 0) {
            System.out.println("No data found.");
            return null;
        } else {
            List<FestSection> sections = new ArrayList<>();
            sections.add(createSection(values));
            schedule = new FestSchedule(sections);
        }
        return schedule;
    }*/

    public static ValueRange getDataRange(String range) throws IOException {
        try {
            return getSheetsService().spreadsheets().values().
                    get(SPREADSHEET_ID, range).
                    execute();
        } catch (URISyntaxException e) {
            logger.error(e.getMessage(), e);
            throw new IOException(e);
        }
    }

}
