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

import org.openhab.binding.gruenbeckcloud.internal.GruenbeckCloudSoftenerConfiguration;

import com.google.gson.annotations.SerializedName;

/**
 * Gruenbeck Cloud Api Device data class.
 *
 * @author Dominik Schön - Initial contribution
 */
public class Device {

    private String id;
    private String series;

    private String serialNumber;

    @SerializedName("has_error")
    private boolean error;
    private String name;
    private Integer type;
    private boolean register;

    private String wsUrl;
    private String wsAccessToken;

    // {
    // "hardwareVersion": "00000003",
    // "lastService": "2019-07-04",
    // "mode": 2,
    // "nextRegeneration": "2020-06-18T00:52:00",
    // "rawWater": 14,
    // "softWater": 3,
    // "softwareVersion": "0001.0032",
    // "errors": [
    // {
    // "isResolved": true,
    // "date": "2020-06-02T02:03:38.612",
    // "message": "Antriebsstörung Steuerventil Regeneration!",
    // "type": "warning"
    // },
    // {
    // "isResolved": true,
    // "date": "2020-05-28T01:30:51.527",
    // "message": "Antriebsstörung Steuerventil Regeneration!",
    // "type": "warning"
    // }
    // ],
    // "salt": [
    // {
    // "date": "2020-06-16",
    // "value": 137
    // },
    // {
    // "date": "2020-06-15",
    // "value": 0
    // },
    // {
    // "date": "2020-06-14",
    // "value": 135
    // }
    // ],
    // "timeZone": "+02:00",
    // "water": [
    // {
    // "date": "2020-06-16",
    // "value": 125
    // },
    // {
    // "date": "2020-06-15",
    // "value": 188
    // },
    // {
    // "date": "2020-06-14",
    // "value": 209
    // }
    // ],
    // "unit": 1,
    // "id": "...",
    // "serialNumber": "...",
    // "name": "Grünbeck",
    // "type": 18,
    // "hasError": false,
    // "register": true
    // }

    public Device(GruenbeckCloudSoftenerConfiguration config) {
        this.id = config.id;
        this.series = config.series;
        this.serialNumber = config.serialNumber;
        this.error = config.error;
        this.name = config.name;
        this.type = config.type;
        this.register = config.register;
    }

    public String getId() {
        return id;
    }

    public String getSerial() {
        return serialNumber;
    }

    public boolean hasError() {
        return error;
    }

    public String getName() {
        return name;
    }

    public Integer getType() {
        return type;
    }

    public String getSeries() {
        return series;
    }
}
