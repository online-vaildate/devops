package io.choerodon.devops.infra.common.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import io.choerodon.core.exception.CommonException;

/**
 * Created by younger on 2018/3/29.
 */
@Component
public class GitUtil {

    private static final String APPLICATION = "application";
    private static final String DEVELOP = "develop";
    private static final String PATH = "/";
    private static final String REPONAME = "devops-service-repo";
    private ResourceLoader resourceLoader = new DefaultResourceLoader();
    private String classPath;

    /**
     * 构造方法
     */
    public GitUtil() {
        try {
            this.classPath = resourceLoader.getResource("/").getURI().getPath();
            String repositoryPath = this.classPath == null ? "" : this.classPath + REPONAME;
            File repo = new File(repositoryPath);
            if (!repo.exists()) {
                repo.mkdirs();
            }
        } catch (IOException io) {
            throw new CommonException(io.getMessage());
        }
    }

    /**
     * Git克隆
     */
    public Git clone(String name, String remoteUrl) {
        Git git = null;
        String workingDirectory = getWorkingDirectory(name);
        File localPathFile = new File(workingDirectory);
        deleteDirectory(localPathFile);
        try {
            git = Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setBranch("master")
                    .setDirectory(localPathFile)
                    .call();
        } catch (GitAPIException e) {
            throw new CommonException("error.git.clone");
        }
        return git;
    }

    /**
     * 将代码推到目标库
     */
    public void push(Git git, String name, String repoUrl, String userName, String accessToken, String type) {
        try {
            String[] url = repoUrl.split("://");
            git.add().addFilepattern(".").call();
            git.add().setUpdate(true).addFilepattern(".").call();
            git.commit().setMessage("Render Variables[skip ci]").call();
            if (type.equals(APPLICATION)) {
                git.branchCreate().setName(DEVELOP).call();
            }
            List<Ref> refs = git.branchList().call();
            PushCommand pushCommand = git.push();
            for (Ref ref : refs) {
                pushCommand.add(ref);
            }
            pushCommand.setRemote(url[0] + "://gitlab-ci-token:" + accessToken + "@" + url[1]);
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                    userName, accessToken));
            pushCommand.call();
        } catch (GitAPIException e) {
            throw new CommonException("error.git.push");
        } finally {
            //删除模板
            deleteWorkingDirectory(name);
            if (git != null) {
                git.close();
            }
        }
    }

    /**
     * 获取工作目录
     */
    public String getWorkingDirectory(String name) {
        String path = this.classPath == null ? REPONAME + PATH + name : this.classPath + REPONAME + PATH + name;
        return path.replace(PATH, File.separator);
    }

    /**
     * 删除工作目录
     */
    public void deleteWorkingDirectory(String name) {
        String path = getWorkingDirectory(name);
        File file = new File(path);
        deleteDirectory(file);
    }

    /**
     * 删除文件
     */
    public void deleteDirectory(File file) {
        if (file.exists()) {
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                throw new CommonException("error.directory.delete");
            }
        }
    }

}
