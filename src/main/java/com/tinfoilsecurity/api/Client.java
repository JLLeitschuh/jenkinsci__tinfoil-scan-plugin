package com.tinfoilsecurity.api;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.tinfoilsecurity.api.Report.Classification;

public class Client {
  public class APIException extends Exception {};

  private static final String API_HOST = "https://www.tinfoilsecurity.com";

  private static final String ENDPOINT_START_SCAN = API_HOST + "/api/v1/sites/{site_id}/scans";
  private static final String ENDPOINT_GET_SCANS  = API_HOST + "/api/v1/sites/{site_id}/scans";
  private static final String ENDPOINT_GET_REPORT = API_HOST + "/api/v1/sites/{site_id}/scans/{scan_id}/report";

  public Client(String accessKey, String secretKey) {
    Unirest.setDefaultHeader("Authorization", "Token token=" + secretKey + ", access_key=" + accessKey);
  }

  public Scan startScan(String siteID) throws APIException {
    HttpResponse<JsonNode> res = null;
    try {
      res = Unirest.post(ENDPOINT_START_SCAN).routeParam("site_id", siteID).asJson();
    }
    catch (UnirestException e) {
      throw new APIException();
    }

    if (200 == res.getStatus()) {
      return scanFromJSON(res.getBody().getObject());
    }
    else {
      throw new APIException();
    }
  }

  public boolean isScanRunning(String siteID, String scanID) throws APIException {
    Map<String, Object> params = Collections.unmodifiableMap(new HashMap<String, Object>() {
      {
        put("page", 1);
        put("per_page", 1);
        put("status", "running");
      }
    });

    HttpResponse<JsonNode> res;

    try {
      res = Unirest.get(ENDPOINT_GET_SCANS).routeParam("site_id", siteID).queryString(params).asJson();
    }
    catch (UnirestException e) {
      throw new APIException();
    }

    if (200 == res.getStatus()) {
      JSONArray scans = res.getBody().getObject().getJSONArray("scans");
      if (scans.length() > 0) {
        return scanID.equals(scans.getJSONObject(0).getString("id"));
      }
      else {
        return false;
      }
    }
    else {
      throw new APIException();
    }
  }

  public Report getReport(String siteID, String scanID) throws APIException {
    Map<String, Object> params = Collections.unmodifiableMap(new HashMap<String, Object>() {
      {
        put("page", 1);
        put("per_page", 1);
      }
    });

    HttpResponse<JsonNode> res;

    try {
      res = Unirest.get(ENDPOINT_GET_REPORT).routeParam("site_id", siteID).routeParam("scan_id", scanID)
          .queryString(params).asJson();
    }
    catch (UnirestException e) {
      throw new APIException();
    }

    if (200 == res.getStatus()) {
      return reportFromJSON(res.getBody().getObject());
    }
    else {
      return null;
    }
  }

  public void close() {
    Unirest.clearDefaultHeaders();
    try {
      Unirest.shutdown();
    }
    catch (IOException e) {}
  }

  private static Scan scanFromJSON(JSONObject object) {
    String scanID = object.getString("id");
    return new Scan(scanID);
  }

  private static Report reportFromJSON(JSONObject object) {
    return new Report(Classification.fromString(object.getString("classification")));
  }
}