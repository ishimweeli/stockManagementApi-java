package com.java.StockMarketApi.Utils;

import com.java.StockMarketApi.Models.User;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;


@Component
public class JsonResponseHelper {

    public JSONObject createSuccess(String message, Object data) throws JSONException {
        JSONObject response = new JSONObject();
        response.put("success", true);
        
        if (message != null && !message.isEmpty()) {
            response.put("message", message);
        }
        
        if (data != null) {
            response.put("data", data);
        }
        
        return response;
    }

    public JSONObject createError(String errorMessage, int statusCode) throws JSONException {
        JSONObject error = new JSONObject();
        error.put("message", errorMessage);
        error.put("code", statusCode);

        JSONObject response = new JSONObject();
        response.put("success", false);
        response.put("error", error);
        
        return response;
    }

    public JSONObject createAuthSuccess(User user, String token) throws JSONException {
        JSONObject userData = new JSONObject();
        userData.put("id", user.getId());
        userData.put("username", user.getUsername());
        userData.put("email", user.getEmail());
        userData.put("role", user.getRole().ordinal());

        JSONObject data = new JSONObject();
        data.put("user", userData);
        data.put("token", token);

        return createSuccess("Authentication successful", data);
    }

}
