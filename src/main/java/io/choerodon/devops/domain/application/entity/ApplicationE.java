package io.choerodon.devops.domain.application.entity;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import io.choerodon.devops.domain.application.entity.gitlab.GitlabProjectE;


/**
 * Created by younger on 2018/3/28.
 */
@Component
@Scope("prototype")
public class ApplicationE {
    private Long id;
    private ProjectE projectE;
    private String name;
    private String code;
    private GitlabProjectE gitlabProjectE;
    private ApplicationTemplateE applicationTemplateE;
    private Boolean isActive;
    private Boolean isSynchro;
    private String groupName;
    private String uuid;
    private String token;
    private String publishLevel;
    private String contributor;
    private String description;

    public ApplicationE() {
    }

    public ApplicationE(Long id) {
        this.id = id;
    }

    public ApplicationE(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * 构造函数
     *
     * @param id     应用Id
     * @param code   应用code
     * @param name   应用name
     * @param active 是否启用
     */
    public ApplicationE(Long id, String code, String name, Boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.isActive = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProjectE getProjectE() {
        return projectE;
    }

    public void setProjectE(ProjectE projectE) {
        this.projectE = projectE;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Boolean getSynchro() {
        return isSynchro;
    }

    public void setSynchro(Boolean synchro) {
        isSynchro = synchro;
    }

    public GitlabProjectE getGitlabProjectE() {
        return gitlabProjectE;
    }

    public void setGitlabProjectE(GitlabProjectE gitlabProjectE) {
        this.gitlabProjectE = gitlabProjectE;
    }

    public ApplicationTemplateE getApplicationTemplateE() {
        return applicationTemplateE;
    }

    public void setApplicationTemplateE(ApplicationTemplateE applicationTemplateE) {
        this.applicationTemplateE = applicationTemplateE;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void initProjectE(Long id) {
        this.projectE = new ProjectE(id);
    }

    public void initApplicationTemplateE(Long id) {
        this.applicationTemplateE = new ApplicationTemplateE(id);
    }

    public void initActive(boolean active) {
        this.isActive = active;
    }

    public void initGitlabProjectEByUrl(String url) {
        this.gitlabProjectE = new GitlabProjectE(url);
    }

    public void initGitlabProjectE(Integer id) {
        this.gitlabProjectE = new GitlabProjectE(id);
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void initUuid(String uuid) {
        this.uuid = uuid;
    }

    public void initSynchro(boolean synchro) {
        this.isSynchro = synchro;
    }

    public String getPublishLevel() {
        return publishLevel;
    }

    public void setPublishLevel(String publishLevel) {
        this.publishLevel = publishLevel;
    }

    public String getContributor() {
        return contributor;
    }

    public void setContributor(String contributor) {
        this.contributor = contributor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
