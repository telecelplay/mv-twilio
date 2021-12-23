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
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + "AC76212344a90c381e1167b4a1d190af36" + "/Messages.json";
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        String message = Integer.toString(number);
        Map<String, String> map = new HashMap<String, String>();
        map.put("To", to);
        map.put("MessagingServiceSid", "MG2b8962bf2b0f196d3ba43919fcf98bac");
        map.put("Body", message);
        map.put("From", "+17604927786");
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(url);
        OutboundSMS record = new OutboundSMS();
        String response = null;
        response = target.request().accept(MediaType.APPLICATION_FORM_URLENCODED).post(Entity.entity(map, MediaType.APPLICATION_FORM_URLENCODED), String.class);
        JSONObject json = new JSONObject(response);
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
