package io.choerodon.devops.app.service.impl;

import org.springframework.stereotype.Service;

import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.devops.api.dto.GitlabUserRequestDTO;
import io.choerodon.devops.app.service.GitlabUserService;
import io.choerodon.devops.domain.application.entity.UserAttrE;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabUserE;
import io.choerodon.devops.domain.application.event.GitlabUserEvent;
import io.choerodon.devops.domain.application.repository.GitlabUserRepository;
import io.choerodon.devops.domain.application.repository.UserAttrRepository;
import io.choerodon.devops.infra.common.util.TypeUtil;
import io.choerodon.devops.infra.config.GitlabConfigurationProperties;

/**
 * Created by Zenger on 2018/3/28.
 */
@Service
public class GitlabUserServiceImpl implements GitlabUserService {

    private GitlabConfigurationProperties gitlabConfigurationProperties;
    private GitlabUserRepository gitlabUserRepository;
    private UserAttrRepository userAttrRepository;


    public GitlabUserServiceImpl(GitlabUserRepository gitlabUserRepository,
                                 GitlabConfigurationProperties gitlabConfigurationProperties,
                                 UserAttrRepository userAttrRepository) {
        this.gitlabUserRepository = gitlabUserRepository;
        this.gitlabConfigurationProperties = gitlabConfigurationProperties;
        this.userAttrRepository = userAttrRepository;
    }

    @Override
    public void createGitlabUser(GitlabUserRequestDTO gitlabUserReqDTO) {

        GitlabUserE createOrUpdateGitlabUserE = gitlabUserRepository.createGitLabUser(
                gitlabConfigurationProperties.getPassword(),
                gitlabConfigurationProperties.getProjectLimit(),
                ConvertHelper.convert(gitlabUserReqDTO, GitlabUserEvent.class));

        if (createOrUpdateGitlabUserE != null) {
            UserAttrE userAttrE = new UserAttrE();
            userAttrE.setId(Long.parseLong(gitlabUserReqDTO.getExternUid()));
            userAttrE.setGitlabUserId(createOrUpdateGitlabUserE.getId().longValue());
            userAttrRepository.insert(userAttrE);
        }
    }

    @Override
    public void updateGitlabUser(GitlabUserRequestDTO gitlabUserReqDTO) {
        UserAttrE userAttrE = userAttrRepository.queryById(TypeUtil.objToLong(gitlabUserReqDTO.getExternUid()));
        gitlabUserRepository.updateGitLabUser(TypeUtil.objToInteger(userAttrE.getGitlabUserId()),
                gitlabConfigurationProperties.getProjectLimit(),
                ConvertHelper.convert(gitlabUserReqDTO, GitlabUserEvent.class));
    }

    @Override
    public void isEnabledGitlabUser(Integer userId) {
        UserAttrE userAttrE = userAttrRepository.queryById(TypeUtil.objToLong(userId));
        gitlabUserRepository.isEnabledGitlabUser(TypeUtil.objToInteger(userAttrE.getGitlabUserId()));
    }

    @Override
    public void disEnabledGitlabUser(Integer userId) {
        UserAttrE userAttrE = userAttrRepository.queryById(TypeUtil.objToLong(userId));
        gitlabUserRepository.disEnabledGitlabUser(TypeUtil.objToInteger(userAttrE.getGitlabUserId()));
    }
}
