package io.choerodon.devops.app.service.impl;

import java.io.IOException;
import java.util.*;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.core.convertor.ConvertPageHelper;
import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.dto.*;
import io.choerodon.devops.app.service.ApplicationInstanceService;
import io.choerodon.devops.domain.application.entity.*;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabPipelineE;
import io.choerodon.devops.domain.application.entity.iam.UserE;
import io.choerodon.devops.domain.application.factory.ApplicationInstanceFactory;
import io.choerodon.devops.domain.application.factory.DevopsEnvCommandFactory;
import io.choerodon.devops.domain.application.factory.DevopsEnvCommandValueFactory;
import io.choerodon.devops.domain.application.repository.*;
import io.choerodon.devops.domain.application.valueobject.PipelineResultV;
import io.choerodon.devops.domain.application.valueobject.ReplaceResult;
import io.choerodon.devops.domain.service.DeployService;
import io.choerodon.devops.infra.common.util.FileUtil;
import io.choerodon.devops.infra.common.util.GenerateUUID;
import io.choerodon.devops.infra.common.util.GitUserNameUtil;
import io.choerodon.devops.infra.common.util.enums.*;
import io.choerodon.devops.infra.dataobject.ApplicationInstancesDO;
import io.choerodon.devops.infra.dataobject.ApplicationLatestVersionDO;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.websocket.Msg;
import io.choerodon.websocket.helper.CommandSender;
import io.choerodon.websocket.helper.EnvListener;
import io.choerodon.websocket.helper.EnvSession;


/**
 * Created by Zenger on 2018/4/12.
 */
@Service
public class ApplicationInstanceServiceImpl implements ApplicationInstanceService {
    private static final String RELEASE_NAME = "ReleaseName";
    private static final String ENV_DISCONNECTED = "error.env.disconnect";
    private static Gson gson = new Gson();

    @Value("${agent.version}")
    private String agentExpectVersion;

    @Autowired
    private ApplicationInstanceRepository applicationInstanceRepository;
    @Autowired
    private ApplicationVersionRepository applicationVersionRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private DevopsEnvironmentRepository devopsEnvironmentRepository;
    @Autowired
    private DeployService deployService;
    @Autowired
    private GitlabProjectRepository gitlabProjectRepository;
    @Autowired
    private IamRepository iamRepository;
    @Autowired
    private CommandSender commandSender;
    @Autowired
    private DevopsEnvCommandRepository devopsEnvCommandRepository;
    @Autowired
    private DevopsEnvCommandValueRepository devopsEnvCommandValueRepository;
    @Autowired
    private EnvListener envListener;

    @Override
    public Page<ApplicationInstanceDTO> listApplicationInstance(Long projectId, PageRequest pageRequest,
                                                                Long envId, Long versionId, Long appId, String params) {
        Map<String, EnvSession> envs = envListener.connectedEnv();
        Page<ApplicationInstanceE> applicationInstanceEPage = applicationInstanceRepository.listApplicationInstance(
                projectId, pageRequest, envId, versionId, appId, params);
        for (ApplicationInstanceE applicationInstanceE : applicationInstanceEPage) {
            for (Map.Entry<String, EnvSession> entry : envs.entrySet()) {
                EnvSession envSession = entry.getValue();
                if (envSession.getEnvId().equals(applicationInstanceE.getDevopsEnvironmentE().getId())) {
                    if (agentExpectVersion.compareTo(envSession.getVersion()) < 1) {
                        applicationInstanceE.setConnect(true);
                    }
                }
            }
        }
        return ConvertPageHelper.convertPage(applicationInstanceEPage, ApplicationInstanceDTO.class);
    }

    @Override
    public List<ApplicationInstancesDTO> listApplicationInstances(Long projectId, Long appId) {
        List<ApplicationInstancesDO> instancesDOS = applicationInstanceRepository.getDeployInstances(projectId, appId);
        List<ApplicationLatestVersionDO> appLatestVersionList =
                applicationVersionRepository.listAppLatestVersion(projectId);
        Map<Long, ApplicationLatestVersionDO> latestVersionList = new HashMap();
        appLatestVersionList.forEach(t -> latestVersionList.put(t.getAppId(), t));
        Map<Long, Integer> appInstancesListMap = new HashMap<>();
        List<ApplicationInstancesDTO> appInstancesList = new ArrayList<>();
        instancesDOS.forEach(t -> {
            ApplicationInstancesDTO instancesDTO = new ApplicationInstancesDTO();
            if (appInstancesListMap.get(t.getAppId()) == null) {
                if (t.getInstanceId() != null
                        || t.getVersionId().equals(latestVersionList.get(t.getAppId()).getVersionId())) {
                    instancesDTO = new ApplicationInstancesDTO(
                            t.getAppId(),
                            t.getPublishLevel(),
                            t.getAppName(),
                            t.getAppCode(),
                            latestVersionList.get(t.getAppId()).getVersionId(),
                            latestVersionList.get(t.getAppId()).getVersion());
                    if (t.getInstanceId() != null) {
                        addAppInstance(instancesDTO, t);
                    }
                    appInstancesListMap.put(t.getAppId(), appInstancesList.size());
                    appInstancesList.add(instancesDTO);
                }
            } else {
                instancesDTO = appInstancesList.get(appInstancesListMap.get(t.getAppId()));
                addInstanceIfNotExist(instancesDTO, t);
            }
            if (t.getInstanceId() != null
                    && t.getVersion().equalsIgnoreCase(instancesDTO.getLatestVersion())) {
                instancesDTO.addLatestVersionRunning();
            }
        });
        return appInstancesList;
    }

    private void addAppInstance(ApplicationInstancesDTO instancesDTO, ApplicationInstancesDO instancesDO) {
        EnvVersionDTO envVersionDTO = new EnvVersionDTO(
                instancesDO.getVersionId(),
                instancesDO.getVersion(),
                instancesDO.getInstanceId(),
                instancesDO.getInstanceCode(),
                instancesDO.getInstanceStatus());
        EnvInstanceDTO envInstanceDTO = new EnvInstanceDTO(instancesDO.getEnvId());
        envInstanceDTO.addEnvVersionDTOS(envVersionDTO);
        instancesDTO.appendEnvInstanceDTOS(envInstanceDTO);
        if (instancesDTO.getLatestVersionId().equals(instancesDO.getVersionId())) {
            instancesDTO.appendInstances(new EnvInstancesDTO(
                    instancesDO.getInstanceId(), instancesDO.getInstanceCode(), instancesDO.getInstanceStatus()));
        }
    }

    private void addInstanceIfNotExist(ApplicationInstancesDTO instancesDTO,
                                       ApplicationInstancesDO instancesDO) {
        EnvInstanceDTO envInstanceDTO = instancesDTO.queryLastEnvInstanceDTO();
        if (instancesDTO.getLatestVersionId().equals(instancesDO.getVersionId())) {
            instancesDTO.appendInstances(new EnvInstancesDTO(
                    instancesDO.getInstanceId(), instancesDO.getInstanceCode(), instancesDO.getInstanceStatus()));
        }
        if (envInstanceDTO.getEnvId().equals(instancesDO.getEnvId())) {
            EnvVersionDTO envVersionDTO = envInstanceDTO.queryLastEnvVersionDTO();
            if (envVersionDTO.getVersion().equals(instancesDO.getVersion())) {
                envVersionDTO.appendInstanceList(
                        instancesDO.getInstanceId(),
                        instancesDO.getInstanceCode(),
                        instancesDO.getInstanceStatus());
            } else {
                envInstanceDTO.addEnvVersionDTOS(new EnvVersionDTO(
                        instancesDO.getVersionId(),
                        instancesDO.getVersion(),
                        instancesDO.getInstanceId(),
                        instancesDO.getInstanceCode(),
                        instancesDO.getInstanceStatus()));
            }
        } else {
            EnvVersionDTO envVersionDTO = new EnvVersionDTO(
                    instancesDO.getVersionId(),
                    instancesDO.getVersion(),
                    instancesDO.getInstanceId(),
                    instancesDO.getInstanceCode(),
                    instancesDO.getInstanceStatus());
            envInstanceDTO = new EnvInstanceDTO(instancesDO.getEnvId());
            envInstanceDTO.addEnvVersionDTOS(envVersionDTO);
            instancesDTO.appendEnvInstanceDTOS(envInstanceDTO);
        }
    }

    @Override
    public ReplaceResult queryValues(Long appId, Long envId, Long versionId) {
        ReplaceResult replaceResult = new ReplaceResult();
        String versionValue = applicationVersionRepository.queryValue(versionId);
        String deployValue = applicationInstanceRepository.queryValueByEnvIdAndAppId(envId, appId);
        replaceResult.setYaml(versionValue);
        if (deployValue != null) {
            replaceResult = FileUtil.replace(versionValue, deployValue);
        }
        try {
            replaceResult.setTotalLine(FileUtil.getFileTotalLine(replaceResult.getYaml()) + 1);
        } catch (IOException e) {
            throw new CommonException(e.getMessage());
        }
        return replaceResult;
    }


    @Override
    public ReplaceResult queryValue(Long instanceId) {
        ReplaceResult replaceResult = new ReplaceResult();
        ApplicationInstanceE applicationInstanceE = applicationInstanceRepository.selectById(instanceId);
        String yaml = applicationInstanceRepository.queryValueByEnvIdAndAppId(
                applicationInstanceE.getDevopsEnvironmentE().getId(), applicationInstanceE.getApplicationE().getId());
        replaceResult.setYaml(yaml);
        try {
            replaceResult.setTotalLine(FileUtil.getFileTotalLine(yaml) + 1);
        } catch (IOException e) {
            throw new CommonException(e.getMessage());
        }
        return replaceResult;
    }

    @Override
    public Boolean create(ApplicationDeployDTO applicationDeployDTO) {
        if (isEnvConnected(applicationDeployDTO.getEnvironmentId())) {
            ApplicationE applicationE = applicationRepository.query(applicationDeployDTO.getAppId());
            DevopsEnvironmentE devopsEnvironmentE =
                    devopsEnvironmentRepository.queryById(applicationDeployDTO.getEnvironmentId());
            ApplicationVersionE applicationVersionE =
                    applicationVersionRepository.query(applicationDeployDTO.getAppVerisonId());
            ApplicationInstanceE applicationInstanceE = ApplicationInstanceFactory.create();
            applicationInstanceE.initApplicationVersionEById(applicationDeployDTO.getAppVerisonId());
            applicationInstanceE.initApplicationEById(applicationDeployDTO.getAppId());
            applicationInstanceE.initDevopsEnvironmentEById(applicationDeployDTO.getEnvironmentId());
            applicationInstanceE.setStatus(InstanceStatus.OPERATIING.getStatus());
            DevopsEnvCommandE devopsEnvCommandE = DevopsEnvCommandFactory.createDevopsEnvCommandE();
            if (applicationDeployDTO.getType().equals("create")) {
                applicationInstanceE.setCode(applicationE.getCode() + "-" + GenerateUUID.generateUUID().substring(0, 5));
                devopsEnvCommandE.setObject(ObjectType.INSTANCE.getObjectType());
                devopsEnvCommandE.setObjectId(applicationInstanceRepository.create(applicationInstanceE).getId());
                devopsEnvCommandE.setCommandType(CommandType.CREATE.getCommandType());
                devopsEnvCommandE.setStatus(CommandStatus.DOING.getCommandStatus());
            }
            if (applicationDeployDTO.getType().equals("update")) {
                ApplicationInstanceE newApplicationInstanceE = applicationInstanceRepository.selectById(
                        applicationDeployDTO.getAppInstanceId());
                applicationInstanceE.setCode(newApplicationInstanceE.getCode());
                devopsEnvCommandE.setObject(ObjectType.INSTANCE.getObjectType());
                devopsEnvCommandE.setObjectId(applicationDeployDTO.getAppInstanceId());
                devopsEnvCommandE.setCommandType(CommandType.UPDATE.getCommandType());
                devopsEnvCommandE.setStatus(CommandStatus.DOING.getCommandStatus());
                applicationInstanceE.setId(applicationDeployDTO.getAppInstanceId());
                applicationInstanceRepository.update(applicationInstanceE);
            }
            DevopsEnvCommandValueE devopsEnvCommandValueE = DevopsEnvCommandValueFactory.createDevopsEnvCommandE();
            devopsEnvCommandValueE.setValue(applicationDeployDTO.getValues());
            devopsEnvCommandE.initDevopsEnvCommandValueE(devopsEnvCommandValueRepository
                    .create(devopsEnvCommandValueE).getId());
            deployService.deploy(
                    applicationE,
                    applicationVersionE,
                    applicationInstanceE,
                    devopsEnvironmentE,
                    applicationDeployDTO.getValues(), applicationDeployDTO.getType(),
                    devopsEnvCommandRepository.create(devopsEnvCommandE).getId());
        } else {
            throw new CommonException(ENV_DISCONNECTED);
        }
        return true;
    }

    @Override
    public List<VersionFeaturesDTO> queryVersionFeatures(Long appInstanceId) {
        ApplicationInstanceE applicationInstanceE = applicationInstanceRepository.selectById(appInstanceId);
        ApplicationE applicationE = applicationRepository.query(
                applicationInstanceE.getApplicationE().getId());

        Integer gitlabProjectId = applicationE.getGitlabProjectE().getId();
        List<GitlabPipelineE> gitlabPipelineEList = gitlabProjectRepository.listPipeline(gitlabProjectId, GitUserNameUtil.getUserId());
        if (gitlabPipelineEList == null) {
            return Collections.emptyList();
        }

        List<PipelineResultV> pipelineResultVS = new ArrayList<>();
        String branch = "";
        long pipelineId = 0;
        for (GitlabPipelineE gitlabPipeline : gitlabPipelineEList) {
            PipelineResultV pipelineResultV = new PipelineResultV();
            GitlabPipelineE gitlabPipelineE = gitlabProjectRepository.getPipeline(
                    gitlabProjectId, gitlabPipeline.getId(), GitUserNameUtil.getUserId());
            if (gitlabPipelineE != null) {
                pipelineResultV.setId(gitlabPipelineE.getId().longValue());
                pipelineResultV.setRef(gitlabPipelineE.getRef());

                UserE userE = iamRepository.queryByLoginName(gitlabPipelineE.getUser().getUsername());
                if (userE != null) {
                    pipelineResultV.setImageUrl(userE.getImageUrl());
                }
            }
            pipelineResultVS.add(pipelineResultV);
        }

        List<PipelineResultV> pipelineResultVList = new ArrayList<>();
        for (PipelineResultV pipelineResult : pipelineResultVS) {
            if (branch.equals(pipelineResult.getRef())
                    && pipelineId >= pipelineResult.getId()) {
                pipelineResultVList.add(pipelineResult);
            }
        }

        Collections.sort(pipelineResultVList);

        return ConvertHelper.convertList(pipelineResultVList, VersionFeaturesDTO.class);

    }

    @Override
    public List<AppInstanceCodeDTO> listByOptions(Long projectId, Long appId, Long appVersionId, Long envId) {
        return ConvertHelper.convertList(applicationInstanceRepository
                .listByOptions(projectId, appId, appVersionId, envId), AppInstanceCodeDTO.class);
    }

    @Override
    public void instanceUpgrade(Long instanceId, String repoURL, String chartName, String chartVersion, String values) {
        ApplicationInstanceE instanceE = applicationInstanceRepository.selectById(instanceId);
        if (isEnvConnected(instanceE.getDevopsEnvironmentE().getId())) {

            String namespace = getNameSpace(instanceE.getDevopsEnvironmentE().getId());
            String releaseName = updateInstanceStatus(instanceId, InstanceStatus.OPERATIING.getStatus());
            DevopsEnvCommandE devopsEnvCommandE = devopsEnvCommandRepository
                    .queryByObject(ObjectType.INSTANCE.getObjectType(), instanceId);
            devopsEnvCommandE.setCommandType(CommandType.UPDATE.getCommandType());
            devopsEnvCommandE.setStatus(CommandStatus.DOING.getCommandStatus());
            devopsEnvCommandRepository.update(devopsEnvCommandE);
            Map<String, String> upgradeMap = new HashMap<>();
            upgradeMap.put(RELEASE_NAME, releaseName);
            upgradeMap.put("RepoURL", repoURL);
            upgradeMap.put("ChartName", chartName);
            upgradeMap.put("ChartVersion", chartVersion);
            upgradeMap.put("Values", values);
            String payload = gson.toJson(upgradeMap);
            Long envId = instanceE.getDevopsEnvironmentE().getId();
            sentInstance(payload, releaseName, HelmType.HelmReleasePreUpgrade.toValue(),
                    namespace, devopsEnvCommandE.getId(), envId);
        } else {
            throw new CommonException(ENV_DISCONNECTED);
        }
    }

    @Override
    public void instanceStop(Long instanceId) {
        ApplicationInstanceE instanceE = applicationInstanceRepository.selectById(instanceId);
        if (isEnvConnected(instanceE.getDevopsEnvironmentE().getId())) {
            if (!instanceE.getStatus().equals(InstanceStatus.RUNNING.getStatus())) {
                throw new CommonException("error.instance.notRunning");
            }
            DevopsEnvCommandE devopsEnvCommandE = devopsEnvCommandRepository
                    .queryByObject(ObjectType.INSTANCE.getObjectType(), instanceId);
            devopsEnvCommandE.setCommandType(CommandType.STOP.getCommandType());
            devopsEnvCommandE.setStatus(CommandStatus.DOING.getCommandStatus());
            devopsEnvCommandRepository.update(devopsEnvCommandE);
            String namespace = getNameSpace(instanceE.getDevopsEnvironmentE().getId());
            String releaseName = updateInstanceStatus(instanceId, InstanceStatus.OPERATIING.getStatus());
            Map<String, String> stopMap = new HashMap<>();
            stopMap.put(RELEASE_NAME, releaseName);
            String payload = gson.toJson(stopMap);
            Long envId = instanceE.getDevopsEnvironmentE().getId();
            sentInstance(payload, releaseName, HelmType.HelmReleaseStop.toValue(),
                    namespace, devopsEnvCommandE.getId(), envId);
        } else {
            throw new CommonException(ENV_DISCONNECTED);
        }
    }

    @Override
    public void instanceStart(Long instanceId) {
        ApplicationInstanceE instanceE = applicationInstanceRepository.selectById(instanceId);
        if (isEnvConnected(instanceE.getDevopsEnvironmentE().getId())) {
            if (!instanceE.getStatus().equals(InstanceStatus.STOPED.getStatus())) {
                throw new CommonException("error.instance.notStop");
            }
            DevopsEnvCommandE devopsEnvCommandE = devopsEnvCommandRepository
                    .queryByObject(ObjectType.INSTANCE.getObjectType(), instanceId);
            devopsEnvCommandE.setCommandType(CommandType.RESTART.getCommandType());
            devopsEnvCommandE.setStatus(CommandStatus.DOING.getCommandStatus());
            devopsEnvCommandRepository.update(devopsEnvCommandE);
            String namespace = getNameSpace(instanceE.getDevopsEnvironmentE().getId());
            String releaseName = updateInstanceStatus(instanceId, InstanceStatus.OPERATIING.getStatus());
            Map<String, String> stopMap = new HashMap<>();
            stopMap.put(RELEASE_NAME, releaseName);
            String payload = gson.toJson(stopMap);
            Long envId = instanceE.getDevopsEnvironmentE().getId();
            sentInstance(payload, releaseName, HelmType.HelmReleaseStart.toValue(),
                    namespace, devopsEnvCommandE.getId(), envId);
        } else {
            throw new CommonException(ENV_DISCONNECTED);
        }
    }

    @Override
    public void instanceDelete(Long instanceId) {
        ApplicationInstanceE instanceE = applicationInstanceRepository.selectById(instanceId);
        if (isEnvConnected(instanceE.getDevopsEnvironmentE().getId())) {
            DevopsEnvCommandE devopsEnvCommandE = devopsEnvCommandRepository
                    .queryByObject(ObjectType.INSTANCE.getObjectType(), instanceId);
            devopsEnvCommandE.setCommandType(CommandType.DELETE.getCommandType());
            devopsEnvCommandE.setStatus(CommandStatus.DOING.getCommandStatus());
            devopsEnvCommandRepository.update(devopsEnvCommandE);
            String namespace = getNameSpace(instanceE.getDevopsEnvironmentE().getId());
            String releaseName = updateInstanceStatus(instanceId, InstanceStatus.OPERATIING.getStatus());
            Map<String, String> deleteMap = new HashMap<>();
            deleteMap.put(RELEASE_NAME, releaseName);
            String payload = gson.toJson(deleteMap);
            Long envId = instanceE.getDevopsEnvironmentE().getId();
            sentInstance(payload, releaseName, HelmType.HelmReleaseDelete.toValue(),
                    namespace, devopsEnvCommandE.getId(), envId);
        } else {
            throw new CommonException(ENV_DISCONNECTED);
        }
    }

    @Override
    public void instanceRollback(Integer version, Long instanceId) {
        ApplicationInstanceE instanceE = applicationInstanceRepository.selectById(instanceId);
        String namespace = getNameSpace(instanceE.getDevopsEnvironmentE().getId());
        String releaseName = updateInstanceStatus(instanceId, InstanceStatus.OPERATIING.getStatus());
        Map rollbackMap = new HashMap();
        rollbackMap.put(RELEASE_NAME, releaseName);
        rollbackMap.put("Version", version);
        String payload = gson.toJson(rollbackMap);
        Long envId = instanceE.getDevopsEnvironmentE().getId();
        sentInstance(payload, releaseName, HelmType.HelmReleaseRollback.toValue(), namespace, null, envId);
    }


    private String getNameSpace(Long envId) {
        return devopsEnvironmentRepository.queryById(envId).getCode();
    }

    private String updateInstanceStatus(Long instanceId, String status) {
        ApplicationInstanceE instanceE = applicationInstanceRepository.selectById(instanceId);
        instanceE.setStatus(status);
        applicationInstanceRepository.update(instanceE);
        return instanceE.getCode();
    }

    @Async
    void sentInstance(String payload, String name, String type, String namespace, Long commandId, Long envId) {
        Msg msg = new Msg();
        msg.setKey("env:" + namespace +".envId:" + envId + ".release:" + name);
        msg.setType(type);
        msg.setPayload(payload);
        msg.setCommandId(commandId);
        commandSender.sendMsg(msg);
    }

    Boolean isEnvConnected(Long envId) {
        Map<String, EnvSession> envs = envListener.connectedEnv();
        List<Long> envIds = new ArrayList<>();
        for (Map.Entry<String, EnvSession> entry : envs.entrySet()) {
            EnvSession envSession = entry.getValue();
            if (agentExpectVersion.compareTo(envSession.getVersion()) < 1) {
                envIds.add(envSession.getEnvId());
            }
        }
        return envIds.contains(envId);
    }
}
