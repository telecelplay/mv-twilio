package org.meveo.twilio;

import java.util.Map;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.meveo.service.storage.RepositoryService;
import org.meveo.model.storage.Repository;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.OutboundSMS;
import java.time.Instant;
import java.time.Duration;

public class VerifyOtp extends Script {

    private static final Logger log = LoggerFactory.getLogger(VerifyOtp.class);

    private CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);

    private RepositoryService repositoryService = getCDIBean(RepositoryService.class);

    private Repository defaultRepo = repositoryService.findDefaultRepository();

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

  
    private static final Duration maxDelay = Duration.ofMinutes(3);

    private static final long maxAttemps = 5;


    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        log.info("verify otp:{} to:{}",otp,to);
        result="invalid_request";
		    OutboundSMS outboundSMS = crossStorageApi.find(defaultRepo, OutboundSMS.class)
          .by("to", to)
          .by("purpose","OTP")
          .by("verificationDate","IS_NULL")
          .by("failureDate","IS_NULL")
          .orderBy("creationDate",false) // order by descending creationDate
          .getResult();
        if(outboundSMS!=null){
          long attempts=outboundSMS.getVerificationAttempts();
          if(attempts>=maxAttemps || Duration.between(outboundSMS.getCreationDate(),Instant.now()).compareTo(maxDelay)>0){
            outboundSMS.setFailureDate(Instant.now());
            result="invalid_request";
          } else if(otp!=null && otp.equals(outboundSMS.getOtpCode())){
            outboundSMS.setVerificationDate(Instant.now());
            result="success";
          } else {
            outboundSMS.setVerificationAttempts(++attempts);
            result="invalid_code";
          }
          try {
            crossStorageApi.createOrUpdate(defaultRepo, outboundSMS);
          } catch (Exception ex) {
                log.error("error updating twilio record :{}", ex.getMessage());
            	result="server_error";
              return;
          }
        }
    }
}
