package com.InfernalFC.helpers;

import com.InfernalFC.models.EventData;
import com.InfernalFC.models.PlayerData;
import com.InfernalFC.models.PointsData;
import com.InfernalFC.models.RankData;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

import static com.InfernalFC.helpers.PointsEnum.*;

@Slf4j
@Singleton
public class DataManager {
    private final static String baseUrl = "https://infernal-osrs.com/api/";
    private RankData[] ranks;

    private final OkHttpClient okHttpClient;
    private final Gson gson;

    @Inject
    private DataManager(OkHttpClient okHttpClient, Gson gson) {
        this.okHttpClient = okHttpClient;
        this.gson = gson;
    }

    /** Blocking GET of a JSON endpoint. Callers already run off the client thread (panel worker threads). */
    private <T> T fetch(String pathAndQuery, Class<T> type) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + pathAndQuery);
        if (url == null) {
            throw new IOException("Bad URL: " + pathAndQuery);
        }
        Request request = new Request.Builder().url(url).build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IOException("Unexpected response " + response.code() + " for " + url.encodedPath());
            }
            try (Reader reader = body.charStream()) {
                return gson.fromJson(reader, type);
            }
        }
    }

    public PlayerData[] GetPlayerData(String searchString) {
        try {
            return fetch("Members?active=1&_start=0&_end=10&Clan_id=1&username="
                    + URLEncoder.encode(searchString, StandardCharsets.UTF_8.toString()), PlayerData[].class);
        } catch (Exception e) {
            log.warn("Infernal API request failed", e);
            return new PlayerData[0];
        }
    }

    public PlayerData[] GetAltData(int id) {
        try {
            return fetch("Members?active=1&_start=0&_end=10&Clan_id=1&parentAccount=" + id, PlayerData[].class);

        } catch (Exception e) {
            log.warn("Infernal API request failed", e);
            return new PlayerData[0];
        }
    }

    public PlayerData GetParentData(int id) {
        try {
            PlayerData[] result = fetch("Members?active=1&_start=0&_end=10&Clan_id=1&id=" + id, PlayerData[].class);
            return result[0];

        } catch (Exception e) {
            log.warn("Infernal API request failed", e);
            return new PlayerData();
        }
    }

    public RankData[] GetRankData() {
        if (ranks != null) {
            return ranks;
        }
        try {
            ranks = fetch("ranks?_start=0&_end=5000&Clan_id=1", RankData[].class);

            return ranks;

        } catch (Exception e) {
            log.warn("Infernal API request failed", e);
            return new RankData[0];
        }
    }

    public PointsData GetOverallPoints() {
        return GetPoints(OVERALL);
    }

    public PointsData GetEhbPoints() {
        return GetPoints(EHB);
    }

    public PointsData GetKcPoints() {
        return GetPoints(KC);
    }

    public PointsData GetPetPoints() {
        return GetPoints(PETS);
    }

    private PointsData GetPoints(PointsEnum score) {
        try {
            String path;

            switch (score) {
                case EHB:
                    path = "Members/killcounts/ehb?_start=0&_end=1&Clan_id=1";
                    break;
                case KC:
                    path = "Members/killcounts/overall?_start=0&_end=1&Clan_id=1";
                    break;
                default:
                    path = "Members/points/overall?_start=0&_end=1&Clan_id=1";
                    break;
                case PETS:
                    path = "Members/pets?_sort=count&_order=DESC&_start=0&_end=1&Clan_id=1";
                    break;
            }

            PointsData[] pointsData = fetch(path, PointsData[].class);

            return pointsData[0];

        } catch (Exception e) {
            log.warn("Infernal API request failed", e);
            return new PointsData();
        }
    }

    public EventData[] GetEvents() {
        try {
            EventData[] eventData  = fetch("Events?_end=10&_order=DESC&_sort=date&_start=0&Clan_id=1", EventData[].class);

            Date today = new Date();
            today.setHours(0);
            today.setMinutes(0);
            today.setSeconds(0);
            eventData = Arrays.stream(eventData).filter(x -> x.getDate().after(today)).toArray(EventData[]::new);
            invertArray(eventData);

            return eventData;
        } catch (Exception e) {
            log.warn("Infernal API request failed", e);
            return new EventData[0];
        }
    }

    private void invertArray(EventData[] array) {
        for (int i = 0; i < array.length / 2; i++) {
            EventData temp = array[i];
            array[i] = array[array.length - 1 - i];
            array[array.length - 1 - i] = temp;
        }
    }
}

enum PointsEnum {
    OVERALL,
    EHB,
    KC,
    PETS
}
