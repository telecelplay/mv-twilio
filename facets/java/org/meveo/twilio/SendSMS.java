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
import org.meveo.service.storage.RepositoryService;
import org.meveo.model.storage.Repository;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.persistence.CEIUtils;
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

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        String TWILIO_SID = "AC76212344a90c381e1167b4a1d190af36";
        String TWILIO_API_KEY = "52a357f6d47ecd115a493ab15e5ab778";
        String url = "https://api.twilio.com/2010-04-01/Accounts/AC76212344a90c381e1167b4a1d190af36/Messages.json";
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        String message = Integer.toString(number);
        Form map = new Form().param("To", to).param("MessagingServiceSid", "MG2b8962bf2b0f196d3ba43919fcf98bac").param("Body", message).param("From", "+17604927786");
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder().nonPreemptive().credentials(TWILIO_SID, TWILIO_API_KEY).build();
        Client client = ClientBuilder.newClient();
        client.register(feature);
        WebTarget target = client.target(url);
        OutboundSMS record = new OutboundSMS();
        Response response = null;
      try{
        response = target.request().post(Entity.form(map), Response.class);
       log.info("Response : {}", response);
      }catch(Exception ex){
        log.error("error while hitting  twilio url :{}", ex.getMessage());
        throw new BusinessException("Something went wrong.Please try after sometime");
         
        
      }
      String value = response.readEntity(String.class);
      log.info("Value : {}", value);
        JSONObject json = new JSONObject(value);
      
        result = json.getString("status");
        record.setTo(to);
        record.setMessage(message);
        record.setResponse(result);
        try {
            crossStorageApi.createOrUpdate(defaultRepo, record);
        } catch (Exception ex) {
            log.error("error updating twilio record :{}", ex.getMessage());
        }
        super.execute(parameters);
    }
}
