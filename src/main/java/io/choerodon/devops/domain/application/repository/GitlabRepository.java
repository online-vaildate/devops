package io.choerodon.devops.domain.application.repository;

import java.util.List;

import io.choerodon.devops.domain.application.entity.gitlab.GitlabGroupE;

/**
 * Created by younger on 2018/3/29.
 */
public interface GitlabRepository {

    void addVariable(Integer gitlabProjectId, String key, String value, Boolean protecteds, Integer userId);

    List<String> listTokenByUserId(Integer gitlabProjectId, String name, Integer userId);

    String createToken(Integer gitlabProjectId, String name, Integer userId);

    GitlabGroupE queryGroupByName(String groupName, Integer userId);

    GitlabGroupE createGroup(GitlabGroupE gitlabGroupE, Integer userId);

    Boolean createFile(Integer projectId, Integer userId);

    void createProtectBranch(Integer projectId, String name, String mergeAccessLevel,
                             String pushAccessLevel, Integer userId);

    void deleteProject(Integer projectId, Integer userId);

    void updateProject(Integer projectId, Integer userId);

}
