package org.meveo.twilio;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Random;
import java.time.Instant;
import java.time.Duration;
import javax.ws.rs.client.*;
import javax.ws.rs.core.*;

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

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

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

        Random rnd = new Random();
        String otp = String.format("%06d", rnd.nextInt(999999));
        String body = String.format(otpMessage, otpAppName, otp);

        Twilio.init(TWILIO_SID, TWILIO_TOKEN);
        Message message = Message
                .creator(
                        new PhoneNumber(to),
                        new PhoneNumber(TWILIO_PHONE_NUMBER),
                        body)
                .create();

        String error = message.getErrorMessage();
        if (error != null) {
            LOG.error("Twilio error: {}", error);
            result = "server_error";
            return;
        }

        Message.Status status = message.getStatus();
        if (Message.Status.ACCEPTED.equals(status)) {
            OutboundSMS outboundSMS = new OutboundSMS();
            outboundSMS.setCreationDate(Instant.now());
            outboundSMS.setPurpose("OTP");
            outboundSMS.setOtpCode(otp);
            outboundSMS.setTo(to);
            outboundSMS.setMessage(body);
            outboundSMS.setResponse(result);
            try {
                crossStorageApi.createOrUpdate(defaultRepo, outboundSMS);
            } catch (Exception e) {
                LOG.error("error updating outboundSMS CEI: {}", e);
                result = "server_error";
            }
        }
    }
}
