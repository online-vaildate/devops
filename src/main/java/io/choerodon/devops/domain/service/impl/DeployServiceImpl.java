package io.choerodon.devops.domain.service.impl;

import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.domain.application.entity.ApplicationE;
import io.choerodon.devops.domain.application.entity.ApplicationInstanceE;
import io.choerodon.devops.domain.application.entity.ApplicationVersionE;
import io.choerodon.devops.domain.application.entity.DevopsEnvironmentE;
import io.choerodon.devops.domain.application.valueobject.Payload;
import io.choerodon.devops.domain.service.DeployService;
import io.choerodon.devops.infra.common.util.enums.HelmType;
import io.choerodon.websocket.Msg;
import io.choerodon.websocket.helper.CommandSender;

/**
 * Created by younger on 2018/4/18.
 */
@Service
public class DeployServiceImpl implements DeployService {

    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private CommandSender commandSender;

    @Value("${services.helm.url}")
    private String helmUrl;

    @Override
    @Async
    public void deploy(ApplicationE applicationE, ApplicationVersionE applicationVersionE, ApplicationInstanceE applicationInstanceE, DevopsEnvironmentE devopsEnvironmentE, String values, String type, Long commandId) {
        Msg msg = new Msg();
        Payload payload = new Payload(
                devopsEnvironmentE.getCode(),
                helmUrl + applicationVersionE.getRepository(),
                applicationE.getCode(),
                applicationVersionE.getVersion(),
                values, applicationInstanceE.getCode());
        msg.setKey("env:" + devopsEnvironmentE.getCode() + ".envId:" + devopsEnvironmentE.getId() + ".release:" + applicationInstanceE.getCode());
        if (type.equals("update")) {
            msg.setType(HelmType.HelmReleasePreUpgrade.toValue());
        } else {
            msg.setType(HelmType.HelmReleasePreInstall.toValue());
        }
        try {
            msg.setPayload(mapper.writeValueAsString(payload));
            msg.setCommandId(commandId);
        } catch (IOException e) {
            throw new CommonException("error.payload.error");
        }
        commandSender.sendMsg(msg);
    }

}
