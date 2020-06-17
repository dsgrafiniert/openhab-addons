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
package org.openhab.binding.gruenbeckcloud.internal.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.jupnp.binding.xml.Descriptor.Device;
import org.openhab.binding.gruenbeckcloud.internal.GruenbeckCloudSoftenerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link GruenbeckCloudSoftenerHandler} is the class to manage
 * GruenbeckCloud API communication for softeners
 * 
 * @author Dominik Schön - Initial contribution
 */
public class GruenbeckCloudSoftenerHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(GruenbeckCloudSoftenerHandler.class);

    public GruenbeckCloudSoftenerHandler(Thing thing) {
        super(thing);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void initialize() {
        // TODO Auto-generated method stub
        logger.debug("Start initializing!");
        GruenbeckCloudSoftenerConfiguration config = getThing().getConfiguration().as(GruenbeckCloudSoftenerConfiguration.class);

        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // TODO Auto-generated method stub

    }

}
