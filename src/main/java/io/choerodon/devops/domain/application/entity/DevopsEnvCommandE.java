package io.choerodon.devops.domain.application.entity;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class DevopsEnvCommandE {

    private Long id;
    private String object;
    private Long objectId;
    private String commandType;
    private String status;
    private String error;
    private DevopsEnvCommandValueE devopsEnvCommandValueE;


    public DevopsEnvCommandE() {
    }


    public DevopsEnvCommandE(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public Long getObjectId() {
        return objectId;
    }

    public void setObjectId(Long objectId) {
        this.objectId = objectId;
    }

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public DevopsEnvCommandValueE getDevopsEnvCommandValueE() {
        return devopsEnvCommandValueE;
    }

    public void initDevopsEnvCommandValueE(Long id) {
        this.devopsEnvCommandValueE = new DevopsEnvCommandValueE(id);
    }
}
