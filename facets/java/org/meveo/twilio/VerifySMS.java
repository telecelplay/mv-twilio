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

public class VerifySMS extends Script {

    private static final Logger log = LoggerFactory.getLogger(VerifySMS.class);

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

    private OutboundSMS getOutboundSMS(String to) {
        return crossStorageApi.find(defaultRepo, OutboundSMS.class).by("to", to).getResult();
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        OutboundSMS outboundSMS = null;
        try {
            outboundSMS = getOutboundSMS(to);
            log.info("response : {}" + outboundSMS.getMessage().equalsIgnoreCase(otp));
        } catch (Exception e) {
            throw new BusinessException(e);
        }
        if (outboundSMS.getMessage().equalsIgnoreCase(otp)) {
            log.info("Inside Success");
            result = "Success";
        } else {
            result = "Failure";
        }
        super.execute(parameters);
    }
}
