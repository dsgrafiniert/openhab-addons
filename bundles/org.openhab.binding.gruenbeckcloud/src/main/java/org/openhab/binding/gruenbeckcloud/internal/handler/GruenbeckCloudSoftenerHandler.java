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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.gruenbeckcloud.internal.GruenbeckCloudBindingConstants;
import org.openhab.binding.gruenbeckcloud.internal.GruenbeckCloudBridgeConfiguration;
import org.openhab.binding.gruenbeckcloud.internal.GruenbeckCloudSoftenerConfiguration;
import org.openhab.binding.gruenbeckcloud.internal.api.model.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link GruenbeckCloudSoftenerHandler} is the class to manage
 * GruenbeckCloud API communication for softeners
 * 
 * @author Dominik Schön - Initial contribution
 */
public class GruenbeckCloudSoftenerHandler extends BaseThingHandler {

    private org.slf4j.Logger logger = LoggerFactory.getLogger(GruenbeckCloudSoftenerHandler.class);
    private Device device;
    private GruenbeckCloudBridgeHandler bridgeHandler;
    private ScheduledFuture<?> refreshTask;

    public GruenbeckCloudSoftenerHandler(final Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Start initializing!");
        final GruenbeckCloudSoftenerConfiguration config = getThing().getConfiguration()
                .as(GruenbeckCloudSoftenerConfiguration.class);
        updateStatus(ThingStatus.ONLINE);
        device = new Device(config);
        final Thing thing = this.getThing();
        logger.debug("Gruenbeck thingHandler thing: {}", config.toString());
        bridgeHandler = (GruenbeckCloudBridgeHandler) getBridge().getHandler();
        startAutomaticRefresh();

    }

    private void startAutomaticRefresh() {
        final Runnable refresher = () -> refreshStateAndUpdate();
        final GruenbeckCloudBridgeConfiguration bridgeConfig = bridgeHandler.getThing().getConfiguration()
                .as(GruenbeckCloudBridgeConfiguration.class);
        this.refreshTask = scheduler.scheduleWithFixedDelay(refresher, 0, bridgeConfig.refreshPeriod, TimeUnit.SECONDS);
        logger.debug("Start automatic refresh at {} seconds", bridgeConfig.refreshPeriod);
    }

    private void refreshStateAndUpdate() {
        bridgeHandler.getDeviceInformation(device);
        bridgeHandler.negotiateWS(device);
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        // TODO Auto-generated method stub

    }

    @Override
    public void dispose() {
        logger.debug("Running dispose()");
        if (this.refreshTask != null) {
            this.refreshTask.cancel(true);
            this.refreshTask = null;
        }
    }

}
