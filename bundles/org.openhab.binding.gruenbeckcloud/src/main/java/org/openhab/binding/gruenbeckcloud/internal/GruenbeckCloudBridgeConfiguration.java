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
package org.openhab.binding.gruenbeckcloud.internal;

/**
 * The {@link GruenbeckCloudBridgeConfiguration} class contains fields mapping Bridge configuration parameters.
 *
 * @author Dominik Schön - Initial contribution
 */
public class GruenbeckCloudBridgeConfiguration {

    public String username;
    public String password;
    public int refreshPeriod;
}
