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

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpContentResponse;
import org.eclipse.jetty.client.HttpResponseException;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Response.ContentListener;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.PathWatcher.EventListListener;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerCallback;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.io.net.http.HttpUtil;
import org.eclipse.smarthome.io.net.http.WebSocketFactory;
import org.ietf.jgss.GSSException;
import org.openhab.binding.gruenbeckcloud.internal.GruenbeckCloudBridgeConfiguration;
import org.openhab.binding.gruenbeckcloud.internal.api.model.Device;
import org.openhab.binding.gruenbeckcloud.internal.api.model.Event;
import org.openhab.binding.gruenbeckcloud.internal.listener.DeviceStatusListener;
import org.openhab.binding.gruenbeckcloud.internal.listener.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link GruenbeckCloudBridgeHandler} is the main class to manage
 * GruenbeckCloud API connection
 * 
 * @author Dominik Schön - Initial contribution
 */
@NonNullByDefault
public class GruenbeckCloudBridgeHandler extends BaseBridgeHandler implements EventListener{

    private final Logger logger = LoggerFactory.getLogger(GruenbeckCloudBridgeHandler.class);
    private final SslContextFactory sslContextFactory = new SslContextFactory();
    private @Nullable HttpClient httpClient;
    private @Nullable GruenbeckCloudBridgeConfiguration config;
    private @Nullable String csrf;
    private @Nullable String transId;
    private @Nullable String policy;
    private @Nullable String tenant;

    private @Nullable String accessToken;
    private @Nullable String refreshToken;
    private @Nullable ScheduledFuture<?> initializeTask, heartbeatTask;

    private long lastUpdate;

    private final Set<DeviceStatusListener> deviceStatusListeners = new CopyOnWriteArraySet<>();


    private @Nullable GruenbeckWebSocket gbWebsocket;

    public GruenbeckCloudBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.info("Start initializing!");
        lastUpdate = System.currentTimeMillis();
        config = getConfigAs(GruenbeckCloudBridgeConfiguration.class);
        httpClient = new HttpClient(sslContextFactory);
        Runnable refresher = () -> asyncInitialize();

        this.initializeTask = scheduler.schedule(refresher, 20, TimeUnit.SECONDS);

        updateStatus(ThingStatus.ONLINE);

    }

    private void asyncInitialize() {
        logger.info("Start async initializing!");

        final CodeChallenge challenge = getCodeChallenge();
        // Instantiate and configure the SslContextFactory

        // Instantiate HttpClient with the SslContextFactory
        try {
            if (httpClient == null)
                httpClient = new HttpClient(sslContextFactory);
 
            httpClient.start();

            initializeHttpClient(challenge);
            sendLoginRequest(challenge);

        } catch (Exception e) {
            logger.error("initialize Error during HTTP communication", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }
    }

    private void sendLoginRequest(CodeChallenge challenge) {
        logger.info("Start sendLoginRequest");
        Request loginRequest = httpClient.newRequest(
                "https://gruenbeckb2c.b2clogin.com" + tenant + "/SelfAsserted?tx=" + transId + "&p=" + policy);
        loginRequest.method(HttpMethod.POST);
        loginRequest.agent(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 12_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.1.2 Mobile/15E148 Safari/604.1");
        loginRequest.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        loginRequest.header("X-CSRF-TOKEN", csrf);
        loginRequest.header("Accept", "application/json, text/javascript, */*; q=0.01");
        loginRequest.header("X-Requested-With", "XMLHttpRequest");
        loginRequest.header("Origin", "https://gruenbeckb2c.b2clogin.com");
        loginRequest.header("Referer",
                "https://gruenbeckb2c.b2clogin.com/a50d35c1-202f-4da7-aa87-76e51a3098c6/b2c_1_signinup/oauth2/v2.0/authorize?state=NzZDNkNBRkMtOUYwOC00RTZBLUE5MkYtQTNFRDVGNTQ3MUNG&x-client-Ver=0.2.2&prompt=select_account&response_type=code&code_challenge_method=S256&x-client-OS=12.4.1&scope=https%3A%2F%2Fgruenbeckb2c.onmicrosoft.com%2Fiot%2Fuser_impersonation+openid+profile+offline_access&x-client-SKU=MSAL.iOS&code_challenge=PkCmkmlW_KomPNfBLYqzBAHWi10TxFJSJsoYbI2bfZE&x-client-CPU=64&client-request-id=FDCD0F73-B7CD-4219-A29B-EE51A60FEE3E&redirect_uri=msal5a83cc16-ffb1-42e9-9859-9fbf07f36df8%3A%2F%2Fauth&client_id=5a83cc16-ffb1-42e9-9859-9fbf07f36df8&haschrome=1&return-client-request-id=true&x-client-DM=iPhone");

        Fields fields = new Fields();
        fields.add("request_type", "RESPONSE");
        fields.add("logonIdentifier", config.username);
        fields.add("password", config.password);
        loginRequest.content(new FormContentProvider(fields));
        ContentResponse loginResponse;
        try {
            loginResponse = loginRequest.send();
            logger.debug("sendLoginRequest response headers: {}", loginResponse.getHeaders());
            logger.debug("sendLoginRequest Result from WS call: {}", loginResponse.getContentAsString());
            sendCombinedSigninAndSignup(challenge);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            // TODO Auto-generated catch block
            logger.error("sendLoginRequest error during login: {}", e.getLocalizedMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }

    }

    private void sendCombinedSigninAndSignup(CodeChallenge challenge) {
        logger.info("Start sendCombinedSigninAndSignup");
        httpClient.setFollowRedirects(false);
        Request request = httpClient.newRequest("https://gruenbeckb2c.b2clogin.com" + tenant
                + "/api/CombinedSigninAndSignup/confirmed?csrf_token=" + csrf + "&tx=" + transId + "&p=" + policy);
        request.method(HttpMethod.GET);
        request.cookie(new HttpCookie("x-ms-cpim-csrf", csrf));
        request.agent(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 12_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.1.2 Mobile/15E148 Safari/604.1");
        request.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

        ContentResponse response = null;
        try {
            logger.debug("sendCombinedSigninAndSignup headers {}", request.toString());
            response = request.send();
            logger.debug("sendCombinedSigninAndSignup response headers: {}", response.getHeaders());
            logger.debug("sendCombinedSigninAndSignup {} Result from WS call: {}", response.getStatus(),
                    response.getContentAsString());
            String resultString = response.getContentAsString();
            int start, end;
            start = resultString.indexOf("code%3d") + 7;
            end = resultString.indexOf(">here") - 1;
            String code = resultString.substring(start, end);
            logger.debug("code: {}", code);
            getTokens(challenge, code);

        } catch (Exception e) {
            logger.error("sendCombinedSigninAndSignup error during login: {}", e.getStackTrace().toString());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);

        }
    }

    private void getTokens(CodeChallenge challenge, String code) {
        logger.info("Start getTokens");
        httpClient.setFollowRedirects(false);
        Request request = httpClient.newRequest("https://gruenbeckb2c.b2clogin.com" + tenant + "/oauth2/v2.0/token");
        request.method(HttpMethod.POST);

        request.agent("Gruenbeck/320 CFNetwork/978.0.7 Darwin/18.7.0");
        request.header("Host", "gruenbeckb2c.b2clogin.com");
        request.header("x-client-SKU", "MSAL.iOS");
        request.header("Accept", "application/json");
        request.header("x-client-OS", "12.4.1");
        request.header("x-app-name", "Grünbeck myProduct");
        request.header("x-client-CPU", "64");
        request.header("x-app-ver", "1.0.4");
        request.header("Accept-Language", "de-de");
        request.header("Accept-Encoding", "br, gzip, deflate");
        request.header("client-request-id", "4719C1AF-93BC-4F7B-8B17-9F298FF2E9AB");
        request.header("x-client-Ver", "0.2.2");
        request.header("x-client-DM", "iPhone");
        request.header("return-client-request-id", "true");
        request.header("cache-control", "no-cache");
        request.header("Connection", "keep-alive");
        request.header("Content-Type", "application/x-www-form-urlencoded");

        Fields fields = new Fields();
        fields.add("client_info", "1");
        fields.add("scope",
                "https://gruenbeckb2c.onmicrosoft.com/iot/user_impersonation openid profile offline_access");
        fields.add("code", code);
        fields.add("grant_type", "authorization_code");
        fields.add("code_verifier", challenge.getResult());
        fields.add("redirect_uri", "msal5a83cc16-ffb1-42e9-9859-9fbf07f36df8://auth");
        fields.add("client_id", "5a83cc16-ffb1-42e9-9859-9fbf07f36df8");
        request.content(new FormContentProvider(fields));

        ContentResponse tokenResponse;
        try {
            tokenResponse = request.send();
            logger.debug("getTokens response headers: {}", tokenResponse.getHeaders());
            logger.debug("getTokens Result from WS call: {}", tokenResponse.getContentAsString());
            JsonObject responseJSON = new Gson().fromJson(tokenResponse.getContentAsString(), JsonObject.class);
            accessToken = responseJSON.get("access_token").getAsString();
            refreshToken = responseJSON.get("refresh_token").getAsString();

        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            // TODO Auto-generated catch block
            logger.error("getTokens error during login: {}", e.getLocalizedMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }

    }

    private void initializeHttpClient(CodeChallenge challenge) {
        ContentResponse response;
        logger.info("Start initializeHttpClient");

        try {
            response = httpClient.GET(
                    "https://gruenbeckb2c.b2clogin.com/a50d35c1-202f-4da7-aa87-76e51a3098c6/b2c_1_signinup/oauth2/v2.0/authorize?state=NzZDNkNBRkMtOUYwOC00RTZBLUE5MkYtQTNFRDVGNTQ3MUNG&x-client-Ver=0.2.2&prompt=select_account&response_type=code&code_challenge_method=S256&x-client-OS=12.4.1&scope=https%3A%2F%2Fgruenbeckb2c.onmicrosoft.com%2Fiot%2Fuser_impersonation+openid+profile+offline_access&x-client-SKU=MSAL.iOS&code_challenge="
                            + challenge.getHash()
                            + "&x-client-CPU=64&client-request-id=FDCD0F73-B7CD-4219-A29B-EE51A60FEE3E&redirect_uri=msal5a83cc16-ffb1-42e9-9859-9fbf07f36df8%3A%2F%2Fauth&client_id=5a83cc16-ffb1-42e9-9859-9fbf07f36df8&haschrome=1&return-client-request-id=true&x-client-DM=iPhone");

            String resultString = response.getContentAsString();
            int start, end;
            start = resultString.indexOf("csrf") + 7;
            end = resultString.indexOf(",", start) - 1;
            csrf = resultString.substring(start, end);
            start = resultString.indexOf("transId") + 10;
            end = resultString.indexOf(",", start) - 1;
            transId = resultString.substring(start, end);
            start = resultString.indexOf("policy") + 9;
            end = resultString.indexOf(",", start) - 1;
            policy = resultString.substring(start, end);
            start = resultString.indexOf("tenant") + 9;
            end = resultString.indexOf(",", start) - 1;
            tenant = resultString.substring(start, end);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("error during login: {}", e.getLocalizedMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);

        }

    };

    private CodeChallenge getCodeChallenge() {
        return new CodeChallenge();
    }

    @Override
    public void dispose() {
        super.dispose();
        logger.info("dispose GruenbeckCloudBridgeHandler");
        try {
            if (this.initializeTask != null) {
                this.initializeTask.cancel(true);
                this.initializeTask = null;
            }
            httpClient.stop();
        } catch (Exception e) {
            logger.error("Error while stopping httpClient: {}", e.getLocalizedMessage());
        }
    }

    private class CodeChallenge {
        private String hash;
        private String result;

        public CodeChallenge() {
            logger.info("Start CodeChallenge");

            hash = "";
            result = "";
            char[] chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
            int charlength = chars.length;
            while (hash == "" || hash.indexOf("+") != -1 || hash.indexOf("/") != -1 || hash.indexOf("=") != -1
                    || result.indexOf("+") != -1 || result.indexOf("/") != -1) {
                result = "";
                for (int i = 64; i > 0; --i)
                    result += chars[(int) Math.floor(Math.random() * charlength)];
                result = Base64.getEncoder().encodeToString(result.getBytes());
                result = result.replaceAll("=", "");

                MessageDigest digest;
                try {
                    digest = MessageDigest.getInstance("SHA-256");
                    digest.update(result.getBytes());
                    hash = Base64.getEncoder().encodeToString(digest.digest());
                    logger.debug("hash: {}", hash);
                    logger.debug("result: {}", result);
                    hash = hash.substring(0, hash.length() - 1);
                    logger.debug("hash: {}", hash);

                } catch (NoSuchAlgorithmException e) {
                    // TODO Auto-generated catch block
                    logger.error("Error in CodeChallenge: {}", e.getMessage());
                }
            }
        }

        public String getHash() {
            return hash;
        };

        public String getResult() {
            return result;
        };
    }

    public List<Device> getDecivesFromGruenbeckCloud() {
        List<Device> devices = new ArrayList();
        if (accessToken != null) {
            logger.info("Start getDevicesFromGruenbeckCloud");
            httpClient.setFollowRedirects(false);
            Request request = httpClient.newRequest("https://prod-eu-gruenbeck-api.azurewebsites.net/api/devices");
            request.method(HttpMethod.GET);
            request.agent(
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 12_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148");
            request.header("Accept", "application/json, text/plain, */*");
            request.header("Accept-Language", "de-de");
            request.header("Host", "prod-eu-gruenbeck-api.azurewebsites.net");
            request.header("Authorization", "Bearer " + accessToken);
            request.header("cache-control", "no-cache");

            ContentResponse response = null;
            try {
                response = request.send();
                logger.debug("response from getDevicesFromGruenbeckCloud {}", response.getContentAsString());
                devices = new Gson().fromJson(response.getContentAsString(), new TypeToken<List<Device>>() {
                }.getType());
            } catch (Exception e) {
                logger.error("error during getDevicesFromGruenbeckCloud {}", e.getStackTrace().toString());
            }
        }
        return devices;
    }

    public void getDeviceInformation(Device device) {
        if (accessToken != null) {
            logger.info("Start getDeviceInformation");
            httpClient.setFollowRedirects(false);
            Request request = httpClient
                    .newRequest("https://prod-eu-gruenbeck-api.azurewebsites.net/api/devices/" + device.getSerial());
            request.method(HttpMethod.GET);
            request.agent(
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 12_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148");
            request.header("Accept", "application/json, text/plain, */*");
            request.header("Accept-Language", "de-de");
            request.header("Host", "prod-eu-gruenbeck-api.azurewebsites.net");
            request.header("Authorization", "Bearer " + accessToken);
            request.header("cache-control", "no-cache");

            ContentResponse response = null;
            try {
                response = request.send();
                logger.debug("response from getDeviceInformation {}", response.getContentAsString());
            } catch (Exception e) {
                logger.error("error during getDeviceInformation {}", e.getStackTrace().toString());
            }
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

    }

    public void negotiateWS(Device device) {
        if (accessToken != null && (gbWebsocket == null || !gbWebsocket.isRunning())) {
            logger.debug("Start negotiateWS");
            httpClient.setFollowRedirects(false);
            Request request = httpClient
                    .newRequest("https://prod-eu-gruenbeck-api.azurewebsites.net/api/realtime/negotiate");
            request.method(HttpMethod.GET);
            request.agent(
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 12_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148");
            request.header("Content-Type", "text/plain;charset=UTF-8");
            request.header("Origin", "file://");
            request.header("Accept", "*/*");
            request.header("Accept-Language", "de-de");
            request.header("Authorization", "Bearer " + accessToken);
            request.header("cache-control", "no-cache");

            ContentResponse response = null;
            try {
                response = request.send();
                JsonObject responseJsonObject = new Gson().fromJson(response.getContentAsString(), JsonObject.class);
                String wsAccessToken = responseJsonObject.get("accessToken").getAsString();
                Request request2 = httpClient.newRequest(
                        "https://prod-eu-gruenbeck-signalr.service.signalr.net/client/negotiate?hub=gruenbeck");
                request2.method(HttpMethod.POST);
                request2.agent("Gruenbeck/349 CFNetwork/1197 Darwin/20.0.0");
                request2.header("content-Type", "text/plain;charset=UTF-8");
                request2.header("accept", "*/*");
                request2.header("Accept-Language", "de-de");
                request2.header("Authorization", "Bearer " + wsAccessToken);
                request2.header("X-Requested-With", "XMLHttpRequest");
                ContentResponse response2 = null;
                response2 = request2.send();
                JsonObject responseJsonObject2 = new Gson().fromJson(response2.getContentAsString(), JsonObject.class);
                String wsConnectionId = responseJsonObject2.get("connectionId").getAsString();
                if (gbWebsocket != null)
                    gbWebsocket.stop();
                gbWebsocket = null;
                gbWebsocket = new GruenbeckWebSocket(this, wsConnectionId, wsAccessToken, device);
                gbWebsocket.start();
                // my $header = {
                // "Content-Length" => 0,
                // "Origin" => "file://",
                // "Accept" => "*/*",
                // "User-Agent" =>
                // "Mozilla/5.0 (iPhone; CPU iPhone OS 12_4_1 like Mac OS X)
                // AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148",
                // "Authorization" => "Bearer " . $hash->{helper}{accessToken},
                // "Accept-Language" => "de-de",
                // "cache-control" => "no-cache"
                // };
                // my $param = {
                // header => $header,
                // url => "https://prod-eu-gruenbeck-api.azurewebsites.net/api/devices/"
                // . ReadingsVal( $name, 'id', '' )
                // . "/realtime/$type?api-version=2019-08-09",
                // callback => \&parseRealtime,
                // hash => $hash,
                // method => "POST"
                // };

              
                enterRealtime(device.getSeries(), device.getSerial());


            } catch (Exception e) {
                logger.error("error during negotiateWS {}", e.getStackTrace().toString());
            }
        } else {
            logger.info("negotiateWS wsSession already established");
        }
        if (lastUpdate < System.currentTimeMillis()-60000){
            logger.info("lastUpdate at lease 60 sec ago. Entering Realtime again");
            enterRealtime(device.getSeries(), device.getSerial());
        }
        asyncHeartbeat(device.getSeries(), device.getId());

    }

    private void enterRealtime (String series, String serial){
        try {
            logger.info("enterRealtime");

            Request request3 = httpClient.newRequest("https://prod-eu-gruenbeck-api.azurewebsites.net/api/devices/" +series+"/" + serial
                 + "/realtime/enter");

                 request3.method(HttpMethod.POST);
                 request3.agent(
                         "Mozilla/5.0 (iPhone; CPU iPhone OS 12_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148");
                 request3.header("Content-Length", "0");
                 request3.header("Origin", "file://");
                 request3.header("Accept", "*/*");
                 request3.header("Accept-Language", "de-de");
                 request3.header("Authorization", "Bearer " + accessToken);
                 request3.header("cache-control", "no-cache");
                 ContentResponse response3 = null;
                     response3 = request3.send();
                 logger.debug("response from realtime/enter {}", response3.getStatus());
                 logger.debug("response from realtime/enter {}", response3.getContentAsString());
        } catch (Exception e) {
            logger.error("error during realtime/enter {}", e.getLocalizedMessage());
        }
    }

    private void asyncHeartbeat(String series, String serial) {
        logger.info("refeshRealtime // heartbeat");

        Request request3 = httpClient.newRequest("https://prod-eu-gruenbeck-api.azurewebsites.net/api/devices/"+ series +"/" + serial + "/realtime/refresh");
        request3.method(HttpMethod.POST);
        request3.agent(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 12_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148");
        request3.header("Content-Length", "0");
        request3.header("Origin", "file://");
        request3.header("Accept", "*/*");
        request3.header("Accept-Language", "de-de");
        request3.header("Authorization", "Bearer " + accessToken);
        request3.header("cache-control", "no-cache");
        logger.debug("request to asyncHeartbeat {}", request3.toString());

        ContentResponse response3 = null;
        try {
            response3 = request3.send();
        logger.debug("response from asyncHeartbeat {}", response3.getStatus());
        logger.debug("response from asyncHeartbeat {}", response3.getContentAsString());

         } catch (Exception e) {
        // TODO Auto-generated catch block
            logger.error("error during hearbeat {}", e.getLocalizedMessage());
        } 
    }

    /**
     * Registers a {@link DeviceStatusListener}.
     *
     * @param deviceStatusListener
     * @return true, if successful
     */
    public boolean registerDeviceStatusListener(final DeviceStatusListener deviceStatusListener) {
        return deviceStatusListeners.add(deviceStatusListener);
    }

    /**
     * Unregisters a {@link DeviceStatusListener}.
     *
     * @param deviceStatusListener
     * @return true, if successful
     */
    public boolean unregisterDeviceStatusListener(final DeviceStatusListener deviceStatusListener) {
        return deviceStatusListeners.remove(deviceStatusListener);
    }

    @Override
    public void onEvent(String msg, Device device) {
        // TODO Auto-generated method stub
        logger.debug("eventListener: String \"{}\"", msg);

        Event e = new Event(msg);

        //TODO: Retrieve device from Event or Message
        logger.debug("eventListener: Event {}", e.toString());

        if (e.getType() == 1){
            logger.debug("eventListener: type 1");
            lastUpdate = System.currentTimeMillis();

            for (DeviceStatusListener listener : deviceStatusListeners){
                
                listener.onDeviceStateChanged(device, e);
            }
        }

    }

    @Override
    public void onError(Throwable cause) {
        // TODO Auto-generated method stub

    }

    @Override
    public void connectionClosed() {
        // TODO Auto-generated method stub

    }

}