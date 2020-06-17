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

import com.google.gson.annotations.SerializedName;

/**
 * Gruenbeck Cloud Api Device data class.
 *
 * @author Dominik Schön - Initial contribution
 */
public class Device {

    private String id;

    private String serialNumber;

    @SerializedName("has_error")
    private boolean error;
    private String name;
    private Integer type;
    private boolean register;

    public String getId() {
        return id;
    }

    public String getSerial(){
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

}
