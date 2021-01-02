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

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.gruenbeckcloud.internal.GruenbeckCloudBridgeConfiguration;
import org.openhab.binding.gruenbeckcloud.internal.GruenbeckCloudSoftenerConfiguration;
import org.openhab.binding.gruenbeckcloud.internal.api.model.Device;
import org.openhab.binding.gruenbeckcloud.internal.api.model.Event;
import org.openhab.binding.gruenbeckcloud.internal.listener.DeviceStatusListener;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.LoggerFactory;

/**
 * The {@link GruenbeckCloudSoftenerHandler} is the class to manage
 * GruenbeckCloud API communication for softeners
 * 
 * @author Dominik Schön - Initial contribution
 */
public class GruenbeckCloudSoftenerHandler extends BaseThingHandler implements DeviceStatusListener {

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(GruenbeckCloudSoftenerHandler.class);
    private final Object lock = new Object();

    private Device device;
    private GruenbeckCloudBridgeHandler bridgeHandler;
    private ScheduledFuture<?> refreshTask;

    public GruenbeckCloudSoftenerHandler(final Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        initializeThing(getBridge() == null ? null : getBridge().getStatus());

        logger.debug("Start initializing!");

        bridgeHandler = (GruenbeckCloudBridgeHandler) getBridge().getHandler();
        startAutomaticRefresh();
    }

    /**
     * Initializes the {@link Thing} corresponding to the given status of the bridge.
     *
     * @param bridgeStatus
     */
    private void initializeThing(@Nullable final ThingStatus bridgeStatus) {
        logger.info("initializeThing thing {} bridge status {}", getThing().getUID(), bridgeStatus);
        final GruenbeckCloudSoftenerConfiguration config = getThing().getConfiguration()
                .as(GruenbeckCloudSoftenerConfiguration.class);

        if (config != null) {
            device = new Device(config);
            final Thing thing = this.getThing();
            logger.debug("Gruenbeck thingHandler thing: {}", config.toString());
            // note: this call implicitly registers our handler as a listener on
            // the bridge
            if (getGruenbeckCloudBridgeHandler() != null) {
                if (bridgeStatus == ThingStatus.ONLINE) {
                    // if (initializeProperties()) {
                    updateStatus(ThingStatus.ONLINE);
                    // } else {
                    // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE,
                    // "Device not found in GruenbeckCloud config. Was it removed?");
                    // }
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                }
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "device id unknown");
        }
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

    private @Nullable GruenbeckCloudBridgeHandler getGruenbeckCloudBridgeHandler() {
        synchronized (this.lock) {
            if (this.bridgeHandler == null) {
                final Bridge bridge = getBridge();
                if (bridge == null) {
                    return null;
                }
                final ThingHandler handler = bridge.getHandler();
                if (handler instanceof GruenbeckCloudBridgeHandler) {
                    this.bridgeHandler = (GruenbeckCloudBridgeHandler) handler;
                    this.bridgeHandler.registerDeviceStatusListener(this);
                } else {
                    return null;
                }
            }
            return this.bridgeHandler;
        }
    }

    @Override
    public void dispose() {
        logger.debug("Running dispose()");
        if (this.refreshTask != null) {
            this.refreshTask.cancel(true);
            this.refreshTask = null;
        }
        if (bridgeHandler != null) {
            bridgeHandler.unregisterDeviceStatusListener(this);
        }
    }

    @Override
    public void onDeviceStateChanged(final Device _device) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onDeviceStateChanged(final Device _device, final Event _event) {
        // TODO Auto-generated method stub
        logger.info("deviceStateChanged: {}, Event {}", _device, _event.getValues());
        for (String key : _event.getValues().keySet()) {
            try {
                logger.debug("deviceStateChanged: {}:{} - {}", key, (Double) _event.getValues().get(key),
                        new DecimalType((Double) _event.getValues().get(key)));
                updateState(key, new DecimalType((Double) _event.getValues().get(key)));
            } catch (Exception e) {
                logger.error("deviceStateChanged: {}:{}, error {}", key, _event.getValues().get(key), e.getMessage());

            }
        }
    }
}
