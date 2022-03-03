package org.meveo.twilio;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Random;
import java.time.Instant;
import java.time.Duration;
import javax.ws.rs.client.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.xml.bind.DatatypeConverter;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.credentials.CredentialHelperService;
import org.meveo.model.customEntities.Credential;
import org.meveo.model.customEntities.OutboundSMS;
import org.meveo.model.persistence.CEIUtils;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.*;

public class SendOtp extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(SendOtp.class);
    private static final String TWILIO_URL = "api.twilio.com";

    private CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private Repository defaultRepo = repositoryService.findDefaultRepository();
    private ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private ParamBean config = paramBeanFactory.getInstance();

    private String retryDelay = config.getProperty("otp.retry.delay", "30");
    private String retryLimit = config.getProperty("otp.retry.limit", "5");
    private String otpAppName = config.getProperty("otp.app.name", "Telecel");
    private String otpMessage = config
            .getProperty("otp.message.format", "Your %s verification code is: %s");
    private Duration retryTimeLimit = Duration.ofSeconds(Long.parseLong(retryDelay));
    private int retryLimitPerDay = Integer.parseInt(retryLimit, 10);

    private String to;
    private String result;

    public String getResult() {
        return result;
    }

    public void setTo(String to) {
        this.to = to;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        List<OutboundSMS> lastSMSList = crossStorageApi.find(defaultRepo, OutboundSMS.class)
                .by("to", to)
                .by("purpose", "OTP")
                .by("fromRange creationDate", Instant.now())
                .orderBy("creationDate", false) // newest to oldest
                .getResults();

        if (lastSMSList != null && lastSMSList.size() > 0) {
            OutboundSMS lastSMS = lastSMSList.get(0);
            Duration lastRetry = Duration.between(lastSMS.getCreationDate(), Instant.now());
            if (lastRetry.compareTo(retryTimeLimit) < 0) {
                result = "retry_later";
                return;
            }
            if (lastSMSList.size() > retryLimitPerDay) {
                result = "too_many_requests";
                return;
            }
        }
        Credential credential =
                CredentialHelperService.getCredential(TWILIO_URL, crossStorageApi, defaultRepo);
        if (credential == null) {
            LOG.error("No credential found for " + TWILIO_URL);
            result = "server_error";
            return;
        } else {
            LOG.info("using credential {} with username {}", credential.getUuid(),
                    credential.getUsername());
        }
        String TWILIO_SID = credential.getUsername();
        String TWILIO_TOKEN = credential.getToken();
        String TWILIO_PHONE_NUMBER = credential.getRefreshToken();

        String url = "https://api.twilio.com/2010-04-01/Accounts/" + TWILIO_SID + "/Messages.json";
        Random rnd = new Random();
        String otp = String.format("%06d", rnd.nextInt(999999));
        String message = String.format(otpMessage, otpAppName, otp);
        LOG.info("Sending OTP {} to {}", otp, to);
        Form map = new Form()
                .param("to", URLEncoder.encode(to, StandardCharsets.UTF_8))
                .param("from", URLEncoder.encode(TWILIO_PHONE_NUMBER, StandardCharsets.UTF_8))
                .param("body", URLEncoder.encode(message, StandardCharsets.UTF_8));
        OutboundSMS outboundSMS = new OutboundSMS();
        String response = null;
        try {
            response = ClientBuilder.newClient()
                    .target(url)
                    .request(MediaType.APPLICATION_FORM_URLENCODED)
                    .header("Authorization", "Basic " + DatatypeConverter.printBase64Binary(
                            (TWILIO_SID + ":" + TWILIO_TOKEN).getBytes(StandardCharsets.UTF_8)))
                    .post(Entity.form(map), Response.class)
                    .readEntity(String.class);
        } catch (Exception e) {
            LOG.error("Sending SMS via Twilio failed: {}", e);
            result = "server_error";
            return;
        }
        LOG.info("response: {}", response);
        JsonObject json = new Gson().fromJson(response, JsonObject.class);
        result = json.get("status").getAsString();
        if ("accepted".equalsIgnoreCase(result)) {
            outboundSMS.setCreationDate(Instant.now());
            outboundSMS.setPurpose("OTP");
            outboundSMS.setOtpCode(otp);
            outboundSMS.setTo(to);
            outboundSMS.setMessage(message);
            outboundSMS.setResponse(result);
            LOG.info("Saving outboundSMS {}", outboundSMS);
            try {
                crossStorageApi.createOrUpdate(defaultRepo, outboundSMS);
            } catch (Exception e) {
                LOG.error("error updating outboundSMS CEI: {}", e);
                result = "server_error";
            }
        } else {
            LOG.error("Sending SMS via Twilio failed: {}", response);
            result = "server_error";
        }
    }
}
