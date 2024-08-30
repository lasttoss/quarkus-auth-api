package io.zw.auth.api.logs;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.zw.auth.api.dto.LogEvent;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventLogger {

    private static final Logger logger = LoggerFactory.getLogger(EventLogger.class);

    private static final Gson gson = new Gson();

    public static void writeToLog(LogEvent request) {
        JsonObject obj = new JsonObject();
        obj.addProperty("userId", request.getUserId());
        obj.addProperty("key", request.getKey());
        obj.addProperty("data", request.getData());
        obj.addProperty("metadata", request.getMetadata());
        obj.addProperty("createdAt", String.valueOf(DateTime.now().toDateTime()));
        logger.debug(gson.toJson(obj));
    }
}
