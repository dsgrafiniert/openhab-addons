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

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Gruenbeck Cloud Api Event data class.
 *
 * @author Dominik Schön - Initial contribution
 */
public class Event {

    private String message;

    private Integer type;

    public Event(String _message) {
        this.message = _message;
        JsonObject jsonObj = new Gson().fromJson(message, JsonObject.class);
        if (jsonObj.has("type")){
            type = jsonObj.get("type").getAsInt();
            if (type == 1){
                
            }
        }
    }

    public String getMessage(){
        return message;
    }

    public void setType(Integer _type){
        type = _type;
    }

    public Integer getType(){
        return type;
    }
    
}

/** 
{
    "type":1,
    "target":"SendMessageToDevice",
    "arguments":[
        {
            "id":"BS40000267",
            "type":"Current",
            "ibuiltindev":true,
            "isncu":"201812110227",
            "mregpercent1":72,
            "mregpercent2":0,
            "mremregstep":0,
            "mregstatus":0,
            "mresidcap1":85,
            "mresidcap2":42,
            "mrescapa1":0.366,
            "mrescapa2":0.184,
            "mmaint":279,
            "mflow1":0,
            "mflow2":0,
            "mflowreg1":0,
            "mflowreg2":0,
            "mflowblend":0,
            "mstep1":4950,
            "mstep2":0,
            "mcurrent":0,
            "mreswatadmod":0,
            "msaltrange":999
        }
    ]
}
*/