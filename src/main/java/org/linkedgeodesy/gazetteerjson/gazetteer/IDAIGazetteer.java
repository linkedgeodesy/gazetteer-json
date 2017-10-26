package org.linkedgeodesy.gazetteerjson.gazetteer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.linkedgeodesy.org.gazetteerjson.json.GGeoJSONFeatureCollection;
import org.linkedgeodesy.org.gazetteerjson.json.GGeoJSONFeatureObject;
import org.linkedgeodesy.org.gazetteerjson.json.GGeoJSONSingleFeature;
import org.linkedgeodesy.org.gazetteerjson.json.NamesJSONObject;

/**
 * JSONObject to store names information
 *
 * @author Florian Thiery
 */
public class IDAIGazetteer {

    public static GGeoJSONSingleFeature getPlaceById(String id) throws IOException, ParseException {
        GGeoJSONSingleFeature json = new GGeoJSONSingleFeature();
        String uri = "https://gazetteer.dainst.org/place/" + id;
        URL url = new URL(uri);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("Accept", "application/vnd.geo+json");
        con.setRequestMethod("GET");
        int responseCode = con.getResponseCode();
        System.out.println("DAI Gazetteer Response Code : " + responseCode + " - " + url);
        if (responseCode < 400) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            JSONObject jsonObject = (JSONObject) new JSONParser().parse(response.toString());
            JSONObject properties = (JSONObject) jsonObject.get("properties");
            JSONArray dainames = (JSONArray) properties.get("names");
            NamesJSONObject names = new NamesJSONObject();
            for (Object item : dainames) {
                JSONObject tmp = (JSONObject) item;
                if (tmp.get("language") != null) {
                    HashSet hs = new HashSet();
                    hs.add(tmp.get("title"));
                    names.setName((String) tmp.get("language"), hs);
                }
            }
            json.setGeometry((JSONObject) jsonObject.get("geometry"));
            json.setProperties(uri, id, "dai", names);
        }
        return json;
    }

    public static GGeoJSONFeatureCollection getPlaceByBBox(String upperleftLat, String upperleftLon, String upperrightLat, String upperrightLon, String lowerrightLat, String lowerrightLon, String lowerleftLat, String lowerleftLon) throws IOException, ParseException {
        GGeoJSONFeatureCollection json = new GGeoJSONFeatureCollection();
        String searchurl = "https://gazetteer.dainst.org/search.json";
        String urlParameters = "?";
        urlParameters += "polygonFilterCoordinates=" + upperleftLon + "&polygonFilterCoordinates=" + upperleftLat + "&polygonFilterCoordinates=" + upperrightLon + "&polygonFilterCoordinates=" + upperrightLat;
        urlParameters += "&polygonFilterCoordinates=" + lowerrightLon + "&polygonFilterCoordinates=" + lowerrightLat + "&polygonFilterCoordinates=" + lowerleftLon + "&polygonFilterCoordinates=" + lowerleftLat;
        urlParameters += "&q=*";
        urlParameters += "&fq=types:populated-place";
        searchurl += urlParameters;
        URL url = new URL(searchurl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        int responseCode = con.getResponseCode();
        System.out.println("DAI Gazetteer Response Code : " + responseCode + " - " + url);
        if (responseCode < 400) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            // ste G-GeoJSON
            JSONObject resultObject = (JSONObject) new JSONParser().parse(response.toString());
            JSONArray result = (JSONArray) resultObject.get("result");
            for (Object item : result) {
                JSONObject tmp = (JSONObject) item;
                // get names
                JSONArray dainames = (JSONArray) tmp.get("names");
                NamesJSONObject names = new NamesJSONObject();
                if (dainames != null) {
                    for (Object item2 : dainames) {
                        JSONObject tmp2 = (JSONObject) item2;
                        if (tmp2.get("language") != null) {
                            HashSet hs = new HashSet();
                            hs.add(tmp2.get("title"));
                            names.setName((String) tmp2.get("language"), hs);
                        }
                    }
                }
                // get geometry
                JSONObject prefLocation = (JSONObject) tmp.get("prefLocation");
                GGeoJSONFeatureObject feature = new GGeoJSONFeatureObject();
                if (prefLocation != null) {
                    JSONArray coordinatesPrefLocation = (JSONArray) prefLocation.get("coordinates");
                    JSONObject geometryObject = new JSONObject();
                    geometryObject.put("type", "Point");
                    geometryObject.put("coordinates", coordinatesPrefLocation);
                    feature.setGeometry(geometryObject); 
                }
                feature.setProperties((String) tmp.get("@id"), (String) tmp.get("gazId"), "dai", names);
                json.setFeature(feature);
            }
            json.setMetadata("dai", upperleftLat, upperleftLon, upperrightLat, upperrightLon, lowerrightLat, lowerrightLon, lowerleftLat, lowerleftLon);
        }
        return json;
    }

}
