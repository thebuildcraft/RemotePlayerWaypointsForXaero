/*
 *    This file is part of the Remote player waypoints for Xaero's Map mod
 *    licensed under the GNU GPL v3 License.
 *    (some parts of this file are originally from "RemotePlayers" by ewpratten)
 *
 *    Copyright (C) 2024  Leander Knüttel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.the_build_craft.remote_player_waypoints_for_xaero.common;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * HTTP utils
 *
 * @author ewpratten
 * @author Leander Knüttel
 * @author eatmyvenom
 * @version 14.06.2024
 */
public class HTTP {

    /**
     * Make an HTTP request, and deserialize
     *
     * @param <T>      Type
     * @param endpoint URL to request from
     * @param clazz    Type class
     * @return Deserialized object
     * @throws IOException
     */
    public static <T> T makeJSONHTTPRequest(URL endpoint, Class clazz) throws IOException {
        // Turn to a Java object
        Gson gson = new Gson();
        return (T) gson.fromJson(makeTextHttpRequest(endpoint), clazz);
    }

    /**
     * Make an HTTP request, and deserialize
     *
     * @param <T>      Type
     * @param endpoint URL to request from
     * @param apiResponseType Type class
     * @return Deserialized object
     * @throws IOException
     */
    public static <T> T makeJSONHTTPRequest(URL endpoint, Type apiResponseType) throws IOException {
        // Turn to a Java object
        Gson gson = new Gson();
        return (T) gson.fromJson(makeTextHttpRequest(endpoint), apiResponseType);
    }

    public static String makeTextHttpRequest(URL url) throws IOException{
        // Open an HTTP request
        HttpURLConnection request = (HttpURLConnection) url.openConnection();
        request.setRequestMethod("GET");
        request.setRequestProperty("Content-Type", "application/json");
        request.setInstanceFollowRedirects(true);

        // Get the content
        BufferedReader responseReader = new BufferedReader(new InputStreamReader(request.getInputStream()));
        StringBuilder response = new StringBuilder();
        String output;
        while ((output = responseReader.readLine()) != null) {
            response.append(output);
        }

        return response.toString();
    }
}