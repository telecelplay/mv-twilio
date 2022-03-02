package org.meveo.twilio;

import java.time.Instant;
import java.time.Duration;
import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.customEntities.OutboundSMS;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerifyOtp extends Script {

  private static final Logger LOG = LoggerFactory.getLogger(VerifyOtp.class);

  private CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
  private RepositoryService repositoryService = getCDIBean(RepositoryService.class);
  private Repository defaultRepo = repositoryService.findDefaultRepository();
  private ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
  private ParamBean config = paramBeanFactory.getInstance();

  private String verificationDelay = config.getProperty("otp.verification.delay", "3");
  private String verificationLimit = config.getProperty("otp.verification.limit", "5");
  private Duration maxDelay = Duration.ofMinutes(Long.parseLong(verificationDelay));
  private long maxAttempts = Long.parseLong(verificationLimit);

  private String otp;
  private String to;
  private String result;

  public String getResult() {
    return result;
  }

  public void setOtp(String otp) {
    this.otp = otp;
  }

  public void setTo(String to) {
    this.to = to;
  }

  @Override
  public void execute(Map<String, Object> parameters) throws BusinessException {
    LOG.info("verify otp:{} to:{}", otp, to);
    result = "invalid_request";
    OutboundSMS latestSMS = crossStorageApi.find(defaultRepo, OutboundSMS.class)
        .by("to", to)
        .by("purpose", "OTP")
        .by("verificationDate", "IS_NULL")
        .by("failureDate", "IS_NULL")
        .orderBy("creationDate", false) // newest to oldest
        .getResult(); // get newest
    if (latestSMS != null) {
      long attempts = latestSMS.getVerificationAttempts();
      Duration delay = Duration.between(latestSMS.getCreationDate(), Instant.now());
      boolean isDelayed = delay.compareTo(maxDelay) > 0;
      LOG.info("creation date: {}", latestSMS.getCreationDate());
      LOG.info("delay: {}", delay);
      LOG.info("isDelayed: {}", isDelayed);
      if (attempts >= maxAttempts || isDelayed) {
        latestSMS.setFailureDate(Instant.now());
        result = "invalid_request";
      } else if (otp != null && otp.equals(latestSMS.getOtpCode())) {
        latestSMS.setVerificationDate(Instant.now());
        result = "success";
      } else {
        latestSMS.setVerificationAttempts(++attempts);
        result = "invalid_code";
      }
      LOG.info("result:{}", result);
      try {
        crossStorageApi.createOrUpdate(defaultRepo, latestSMS);
      } catch (Exception ex) {
        LOG.error("error updating twilio record :{}", ex.getMessage());
        result = "server_error";
        return;
      }
    }
  }
}
