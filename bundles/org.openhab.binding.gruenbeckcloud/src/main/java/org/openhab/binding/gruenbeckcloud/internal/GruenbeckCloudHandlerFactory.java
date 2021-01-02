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

import static org.openhab.binding.gruenbeckcloud.internal.GruenbeckCloudBindingConstants.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.auth.client.oauth2.OAuthFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.gruenbeckcloud.internal.discovery.GruenbeckCloudDiscoveryService;
import org.openhab.binding.gruenbeckcloud.internal.handler.GruenbeckCloudBridgeHandler;
import org.openhab.binding.gruenbeckcloud.internal.handler.GruenbeckCloudSoftenerHandler;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link GruenbeckCloudHandlerFactory} is responsible for creating things
 * and thing handlers.
 *
 * @author Dominik Schön - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.gruenbeckcloud", service = ThingHandlerFactory.class)
public class GruenbeckCloudHandlerFactory extends BaseThingHandlerFactory {

    private @NonNullByDefault({}) OAuthFactory oAuthFactory;
    private @NonNullByDefault({}) HttpClient httpClient;

    private Map<ThingUID, ServiceRegistration<DiscoveryService>> discoveryServiceRegistrations = new HashMap<>();

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return THING_TYPE_GBC_SOFTENER.equals(thingTypeUID) || THING_TYPE_GBC_BRIDGE.equals(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_GBC_SOFTENER.equals(thingTypeUID)) {
            return new GruenbeckCloudSoftenerHandler(thing);
        } else 
        if (THING_TYPE_GBC_BRIDGE.equals(thingTypeUID)) {
            GruenbeckCloudBridgeHandler handler = new GruenbeckCloudBridgeHandler((Bridge) thing);
            registerAccountDiscoveryService(handler, (Bridge) thing);
            return handler;
        }

        return null;
    }

    private void registerAccountDiscoveryService(GruenbeckCloudBridgeHandler handler, Bridge bridge) {
        GruenbeckCloudDiscoveryService discoveryService = new GruenbeckCloudDiscoveryService(handler, bridge);

        ServiceRegistration<DiscoveryService> serviceRegistration = this.bundleContext
                .registerService(DiscoveryService.class, discoveryService, null);

        discoveryServiceRegistrations.put(handler.getThing().getUID(), serviceRegistration);
    }

}
