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
package org.openhab.binding.gruenbeckcloud.internal.discovery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.openhab.binding.gruenbeckcloud.internal.GruenbeckCloudBindingConstants;
import org.openhab.binding.gruenbeckcloud.internal.api.model.Device;
import org.openhab.binding.gruenbeckcloud.internal.handler.GruenbeckCloudBridgeHandler;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link GruenbeckCloudDiscoveryService} is responsible for starting the discovery procedure
 * that connects to Grueneck Cloud and imports all registered softener devices.
 *
 * @author Dominik Schön - Initial contribution
 */
public class GruenbeckCloudDiscoveryService extends AbstractDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(GruenbeckCloudDiscoveryService.class);

    private static final int TIMEOUT = 15;

    private GruenbeckCloudBridgeHandler handler;
    private ThingUID bridgeUID;

    private ScheduledFuture<?> scanTask;

    public GruenbeckCloudDiscoveryService(GruenbeckCloudBridgeHandler hBridgeHandler, Bridge bridge) {
        super(GruenbeckCloudBindingConstants.DISCOVERABLE_THING_TYPE_UIDS, TIMEOUT);
        logger.debug("new GruenbeckCloud DiscoveryService instance created");
        this.handler = hBridgeHandler;
        this.bridgeUID = bridge.getUID();
    }

    @Override
    protected void startScan() {
        logger.debug("starting gruenbeckCloud Discovery");

        if (this.scanTask != null) {
            scanTask.cancel(true);
        }
        this.scanTask = scheduler.schedule(() -> findDevices(), 0, TimeUnit.SECONDS);
    }

    @Override
    protected void startBackgroundDiscovery() {
        findDevices();
    }

    @Override
    protected void stopScan() {
        super.stopScan();

        if (this.scanTask != null) {
            this.scanTask.cancel(true);
            this.scanTask = null;
        }
    }

    private void findDevices() {
        List<Device> devices = handler.getDecivesFromGruenbeckCloud();
        for (Device device : devices) {
            addThing(device);
        }
    }

    private void addThing(Device device) {
        logger.debug("addThing(): Adding new Gruenbeck Cloud unit {} to the smarthome inbox", device.getName());

        Map<String, Object> properties = new HashMap<>();
        ThingUID thingUID = new ThingUID(GruenbeckCloudBindingConstants.THING_TYPE_GBC_SOFTENER, device.getSerial());
        // properties.put(NeatoBindingConstants.CONFIG_SECRET, robot.getSecretKey());
        properties.put(GruenbeckCloudBindingConstants.CONFIG_SERIAL, device.getSerial());
        properties.put(GruenbeckCloudBindingConstants.CONFIG_SERIES, device.getSeries());
        properties.put(GruenbeckCloudBindingConstants.CONFIG_ID, device.getId());
        properties.put(GruenbeckCloudBindingConstants.CONFIG_NAME, device.getName());

        thingDiscovered(DiscoveryResultBuilder.create(thingUID)
                .withThingType(GruenbeckCloudBindingConstants.THING_TYPE_GBC_SOFTENER).withBridge(bridgeUID)
                .withLabel(device.getName() + " (" + device.getSerial() + ")")
                .withRepresentationProperty(device.getSerial()).withProperties(properties).build());
    }
}
