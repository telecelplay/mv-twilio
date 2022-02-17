package org.meveo.twilio;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Random;
import java.time.Instant;
import java.time.Duration;
import org.meveo.service.script.Script;
import org.meveo.model.customEntities.OutboundSMS;
import org.meveo.admin.exception.BusinessException;
import org.slf4j.LoggerFactory;
import org.json.*;
import javax.ws.rs.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ws.rs.core.*;
import org.meveo.model.customEntities.Credential;
import org.meveo.service.storage.RepositoryService;
import org.meveo.model.storage.Repository;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.persistence.CEIUtils;
import org.meveo.credentials.CredentialHelperService;

public class SendOtp extends Script {

    private String to;

    private String result;

    public String getResult() {
        return result;
    }

    private static final Logger log = LoggerFactory.getLogger(SendOtp.class);

    private CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);

    private RepositoryService repositoryService = getCDIBean(RepositoryService.class);

    private Repository defaultRepo = repositoryService.findDefaultRepository();
  
  
	static final private String TWILIO_URL = "api.twilio.com";
  
    static final private Duration minDuration=Duration.ofSeconds(30);
  
    static final private int maxMessageInDay = 5;
  

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        List<OutboundSMS> lastSMSList = crossStorageApi.find(defaultRepo, OutboundSMS.class)
          .by("to", to)
          .by("purpose", "OTP")
          .by("fromRange creationDate", Instant.now())
          .orderBy("creationDate", false) //order by descending creation date to retriev the newest one
          .getResults();
 
        if(lastSMSList!=null && lastSMSList.size()>0){
          OutboundSMS lastSMS=lastSMSList.get(0);
          Duration duration = Duration.between(lastSMS.getCreationDate(), Instant.now());
          if(duration.compareTo(minDuration)<0){
        	//throw new BusinessException("There is an ogoing Otp, please wait.");
            result="retry_later";
            return;
          }
          if(lastSMSList.size()>=maxMessageInDay){
            result="too_many_requests";
            return;
          }
        }
		Credential credential  = CredentialHelperService.getCredential(TWILIO_URL,crossStorageApi,defaultRepo);
      	if(credential==null){
        	//throw new BusinessException("No credential found for "+TWILIO_URL);
            log.error("No credential found for "+TWILIO_URL);
            result="server_error";
            return;
      	} else {
        	log.info("using credential {} with username {}",credential.getUuid(),credential.getUsername());
      	}
        String TWILIO_SID = credential.getUsername();
        String TWILIO_MESSAGE_ID = credential.getToken();
        String from =  credential.getRefreshToken();
        String url = "https://api.twilio.com/2010-04-01/Accounts/"+TWILIO_SID+"/Messages.json";
        Random rnd = new Random();
        String otp =  String.format("%06d", rnd.nextInt(999999));
        String message = "Your telecelplay verification code is:"+otp;
        Form map = new Form().param("To", to).param("MessagingServiceSid", TWILIO_MESSAGE_ID).param("Body", message).param("From", from);
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(url);
        OutboundSMS record = new OutboundSMS();
        Response response = null;
        try {
		    response = CredentialHelperService.setCredential(target.request(),credential).post(Entity.form(map), Response.class);
            log.info("Response : {}", response);
        } catch (Exception ex) {
            log.error("error while hitting  twilio url :{}", ex.getMessage());
            //throw new BusinessException("Something went wrong.Please try after sometime");
            result="server_error";
            return;
        }
        String value = response.readEntity(String.class);
        JSONObject json = new JSONObject(value);
        result = json.getString("status");
        if ("accepted".equalsIgnoreCase(result)) {
            log.info("Value : {}", value);
            record.setCreationDate(Instant.now());
            record.setPurpose("OTP");
            record.setOtpCode(otp);
            record.setTo(to);
            record.setMessage(message);
            record.setResponse(result);
            try {
                crossStorageApi.createOrUpdate(defaultRepo, record);
            } catch (Exception ex) {
                log.error("error updating twilio record :{}", ex.getMessage());
            	result="server_error";
            }
        }
    }

    public void setTo(String to) {
        this.to = to;
    }
}
