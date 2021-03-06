package org.meveo.twilio;

import java.util.Map;
import java.util.HashMap;
import java.util.Random;
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
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

public class SendSMS extends Script {

    private String to;

    private String result;

    public String getResult() {
        return result;
    }

    private static final Logger log = LoggerFactory.getLogger(SendSMS.class);

    private CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);

    private RepositoryService repositoryService = getCDIBean(RepositoryService.class);

    private Repository defaultRepo = repositoryService.findDefaultRepository();
  
  
	static final private String TWILIO_URL = "api.twilio.com";

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
		Credential credential  = CredentialHelperService.getCredential(TWILIO_URL,crossStorageApi,defaultRepo);
      	if(credential==null){
        	throw new BusinessException("No credential found for "+TWILIO_URL);
      	} else {
        	log.info("using credential {} with username {}",credential.getUuid(),credential.getUsername());
      	}
        String TWILIO_SID = credential.getPassword();
        String TWILIO_API_KEY = credential.getApiKey();
        String TWILIO_MESSAGE_ID = credential.getToken();
        String url = "https://api.twilio.com/2010-04-01/Accounts/"+TWILIO_SID+"/Messages.json";
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        String message = Integer.toString(number);
        Form map = new Form().param("To", to).param("MessagingServiceSid", TWILIO_MESSAGE_ID).param("Body", message).param("From", "+12546138615");
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder().nonPreemptive().credentials(TWILIO_SID, TWILIO_API_KEY).build();
        Client client = ClientBuilder.newClient();
        client.register(feature);
        WebTarget target = client.target(url);
        OutboundSMS record = new OutboundSMS();
        Response response = null;
        try {
            response = target.request().post(Entity.form(map), Response.class);
            log.info("Response: {}", response);
        } catch (Exception ex) {
            log.error("error while hitting  twilio url :{}", ex.getMessage());
            throw new BusinessException("Something went wrong.Please try after sometime");
        }
        String value = response.readEntity(String.class);
        JSONObject json = new JSONObject(value);
        if (!json.getString("status").equalsIgnoreCase("accepted")) {
            result = json.getString("message");
        }
        if (json.getString("status").equalsIgnoreCase("accepted")) {
            log.info("Value : {}", value);
            result = json.getString("status");
            record.setTo(to);
            record.setMessage(message);
            record.setResponse(result);
            try {
                crossStorageApi.createOrUpdate(defaultRepo, record);
            } catch (Exception ex) {
                log.error("error updating twilio record :{}", ex.getMessage());
            }
        }
        super.execute(parameters);
    }

    public void setTo(String to) {
        this.to = to;
    }
}
