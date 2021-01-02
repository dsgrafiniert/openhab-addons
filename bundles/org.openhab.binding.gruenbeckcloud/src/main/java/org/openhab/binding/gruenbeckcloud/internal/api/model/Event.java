/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.gruenbeckcloud.internal.api.model;

import java.lang.reflect.Type;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * Gruenbeck Cloud Api Event data class.
 *
 * @author Dominik Schön - Initial contribution
 */
public class Event {

    private String message;

    private Integer type;

    private Map<String, Object> values;

    /**
     * 20:42:48.828 [DEBUG] [handler.GruenbeckCloudSoftenerHandler] - deviceStateChanged:
     * org.openhab.binding.gruenbeckcloud.internal.api.model.Device@4fee14a2, Event
     * 
     * {
     * id=BS40000267,
     * type=CurrSlow,
     * ibuiltindev=true,
     * isncu=201812110227,
     * mcountreg=276.0,
     * mcountwater1=109908.0,
     * mcountwater2=0.0,
     * mcountwatertank=118.0,
     * msaltusage=49.4007,
     * mflowexc=0.0,
     * mflowexc2reg1=0.0,
     * mflowexc1reg2=0.0,
     * mlifeadsorb=0.0,
     * mhardsoftw=5.0,
     * mcapacity=6.0,
     * maverage=999.0,
     * mstddev=0.0,
     * mmax=0.0,
     * mpress=0.0,
     * mtemp=0.0,
     * mflowmax=2.07,
     * mflowmax1reg2=0.0,
     * mflowmax2reg1=0.0,
     * mendreg1=01:18,
     * mendreg2=00:00
     * }
     * 
     * {
     * "type":1,
     * "target":"SendMessageToDevice",
     * "arguments":[
     * {
     * "id":"BS40000267",
     * "type":"Current",
     * "ibuiltindev":true,
     * "isncu":"201812110227",
     * "mregpercent1":72,
     * "mregpercent2":0,
     * "mremregstep":0,
     * "mregstatus":0,
     * "mresidcap1":85,
     * "mresidcap2":42,
     * "mrescapa1":0.366,
     * "mrescapa2":0.184,
     * "mmaint":279,
     * "mflow1":0,
     * "mflow2":0,
     * "mflowreg1":0,
     * "mflowreg2":0,
     * "mflowblend":0,
     * "mstep1":4950,
     * "mstep2":0,
     * "mcurrent":0,
     * "mreswatadmod":0,
     * "msaltrange":999
     * }
     * ]
     * }
     */

    public Event(String _message) {
        this.message = _message;
        JsonObject jsonObj = new Gson().fromJson(message, JsonObject.class);
        if (jsonObj.has("type")) {
            type = jsonObj.get("type").getAsInt();
            if (type == 1) {
                Type stringObjectMap = new TypeToken<ArrayList<Map<String, Object>>>() {
                }.getType();
                ArrayList<Map<String, Object>> args = new Gson().fromJson(jsonObj.get("arguments"), stringObjectMap);
                values = args.get(0);
            }
        }
    }

    public String toString() {
        String returnString = "{type: " + type + ", arguments: {";
        for (String key : values.keySet()) {
            returnString += key + ": " + values.get(key) + ", ";
        }
        returnString += "}}";
        return returnString;
    }

    public String getMessage() {
        return message;
    }

    public void setType(Integer _type) {
        type = _type;
    }

    public Integer getType() {
        return type;
    }

    public Map<String, Object> getValues() {
        return values;
    }
}
