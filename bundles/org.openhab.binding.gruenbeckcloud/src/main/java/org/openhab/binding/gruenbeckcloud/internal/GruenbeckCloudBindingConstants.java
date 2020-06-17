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

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link GruenbeckCloudBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Dominik Schön - Initial contribution
 */
@NonNullByDefault
public class GruenbeckCloudBindingConstants {

    private static final String BINDING_ID = "gruenbeckcloud";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_GBC_SOFTENER = new ThingTypeUID(BINDING_ID, "softener");
    public static final ThingTypeUID THING_TYPE_GBC_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");

    public static final Set<ThingTypeUID> DISCOVERABLE_THING_TYPE_UIDS = Collections
            .singleton(THING_TYPE_GBC_SOFTENER);



    public static final String CONFIG_ID = "id";
    public static final String CONFIG_SERIAL = "serial";
    public static final String CONFIG_NAME = "name";
    public static final String CONFIG_ERROR = "error";

    // List of all Channel ids
    public static final String CHANNEL_1 = "channel1";

       // Authorization related Servlet and resources aliases.
       public static final String GBC_ALIAS = "/connectgruenbeckcloud";
       public static final String GBC_IMG_ALIAS = "/img";
   
}
