package world.weibiansanjue.doctools.javadochub.repository;

import cn.hutool.extra.compress.CompressUtil;
import cn.hutool.extra.compress.extractor.Extractor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 仓库操作.
 *
 * @author weibiansanjue
 * @since 0.1.0
 */
@Slf4j
@Component("repository")
public class Repository implements IRepository, CommandLineRunner {

    @Value("${javadochub.storage}/storage")
    private String LOCAL_PATH;

    @Value("${javadochub.storage}/javadoc")
    private String JAR_PATH;

    @Value("${javadochub.maven.repository}")
    private String REPOSITORY_URL;

    @Value("${javadochub.maven.internal}")
    private String INTERNAL_URL;

    @Value("${javadochub.maven.internal-group-prefix}")
    private List<String> INTERNAL_GROUP_PREFIX;

    private RestTemplate restTemplate;

    public Repository() {
        restTemplate = new RestTemplate();
    }

    /**
     * 获取本地 artifact 名词集合.
     *
     * @author weibiansanjue
     * @param groupId group id
     * @return artifact name list
     * @since 0.1.0
     */
    public List<String> artifact(String groupId) {
        File groupRoot = Paths.get(LOCAL_PATH + File.separator + groupId).toFile();
        if (!groupRoot.exists()) {
            return new ArrayList<>(0);
        }
        File[] files = groupRoot.listFiles(File::isDirectory);
        if (ArrayUtils.isEmpty(files)) {
            return new ArrayList<>(0);
        }
        return Arrays.stream(files).map(File::getName).collect(Collectors.toList());
    }

    /**
     * 获取 maven 仓库中 javadoc 版本.
     *
     * @author weibiansanjue
     * @param groupId group id
     * @param artifactId artifact id
     * @return remote metadata
     * @since 0.1.0
     */
    public Versioning version(String groupId, String artifactId) {
        String url = metadataUrl(groupId, artifactId);
        log.info("remote version. url={}", url);

        ResponseEntity<Metadata> resp = null;
        try {
            resp = restTemplate.getForEntity(url, Metadata.class);
        } catch (HttpClientErrorException hcee) {
            log.error("javadoc version 列表获取失败. hcee={}", hcee.getMessage());
            return null;
        }
        if (!resp.getStatusCode().is2xxSuccessful()) {
            return null;
        }
        Versioning versioning = resp.getBody().getVersioning();
        Collections.reverse(versioning.getVersions());
        if(StringUtils.isEmpty(versioning.getRelease())) {
            versioning.setRelease(versioning.getVersions().get(0));
        }
        return versioning;
    }

    @Override
    public Versioning version(String groupId, String artifactId, String version) {
        return null;
    }

    /**
     * metadata url.
     *
     * @author weibiansanjue
     * @param groupId group id
     * @param artifactId artifact id
     * @return remote metadata url
     * @since 0.2.0
     */
    public String metadataUrl(String groupId, String artifactId) {
        return String.format("%s/%s/%s/%s",
                             repositoryUrl(groupId),
                             groupId.replace(".", "/"),
                             artifactId,
                             "maven-metadata.xml");
    }

    /**
     * 删除 javadoc page
     *
     * @author weibiansanjue
     * @param groupId group id
     * @param artifactId artifact id
     * @param version version
     * @return 删除成功
     * @throws IOException io e
     * @since 0.2.0
     */
    public boolean delete(String groupId, String artifactId, String version) throws IOException {
        Path docFile = Paths.get(LOCAL_PATH + File.separator +
                groupId + File.separator + artifactId + File.separator + version);
        Files.walk(docFile)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        return true;
    }

    /**
     * 下载、解压 javadoc.jar.
     *
     * @author weibiansanjue
     * @param groupId group id
     * @param artifactId artifact id
     * @param version version
     * @return status
     * @throws IOException io exception
     * @since 0.1.0
     */
    public int store(String groupId, String artifactId, String version) throws IOException {
        File docFile = Paths.get(LOCAL_PATH + File.separator +
            groupId + File.separator + artifactId + File.separator + version).toFile();
        if (docFile.exists() && docFile.isDirectory() && ArrayUtils.isNotEmpty(docFile.listFiles())) {
            return 202;
        }

        String url = javadocDownloadUrl(groupId, artifactId, version);
        log.info("download javadoc. url={}", url);
        ResponseEntity<byte[]> resp = null;
        try {
            resp = restTemplate.getForEntity(url, byte[].class);
        } catch (HttpClientErrorException hcee) {
            log.error("javadoc 下载失败. hcee={}", hcee.getMessage());
            return hcee.getRawStatusCode();
        } catch (Exception e) {
            log.error("javadoc 下载失败. e={}", e.getMessage());
            return 500;
        }
        if (!resp.getStatusCode().is2xxSuccessful()) {
            return 500;
        }
        Path versionPath = Paths.get(JAR_PATH + File.separator +
            groupId + File.separator + artifactId + File.separator + version +
                javadocJarName(artifactId, version));
        Path jarPath = versionPath.resolve(javadocJarName(artifactId, version));
        versionPath.toFile().mkdirs();
        Files.write(jarPath, resp.getBody());
        log.info("download javadoc jar. local_path={}", jarPath);

        Extractor extractor = CompressUtil.createExtractor(StandardCharsets.UTF_8, jarPath.toFile());
        extractor.extract(docFile);
        log.info("discompress javadoc jar. local_path={}", docFile.getAbsolutePath());
        Files.delete(jarPath);
        return 202;
    }

    /**
     * javadoc jar download url.
     *
     * @author weibiansanjue
     * @param groupId group id
     * @param artifactId artifact id
     * @param version version
     * @return url
     * @since 0.2.0
     */
    public String javadocDownloadUrl(String groupId, String artifactId, String version) {
        return String.format("%s/%s/%s/%s/%s",
                             repositoryUrl(groupId),
                             groupId.replace(".", "/"),
                             artifactId, version,
                             javadocJarName(artifactId, version));
    }

    @Override
    public Boolean isInternal(String groupId) {
        for (String prefix : INTERNAL_GROUP_PREFIX) {
            if (groupId.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String javadocJarName(String artifactId, String version) {
        return artifactId + "-" + version + "-javadoc.jar";
    }

    private String repositoryUrl(String groupId) {
        if (CollectionUtils.isEmpty(INTERNAL_GROUP_PREFIX) || StringUtils.isEmpty(INTERNAL_URL)) {
            return REPOSITORY_URL;
        }
        for (String prefix : INTERNAL_GROUP_PREFIX) {
            if (groupId.startsWith(prefix)) {
                return INTERNAL_URL;
            }
        }
        return REPOSITORY_URL;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("javadochub storage. path={}", LOCAL_PATH);
        log.info("javadochub remote repository. url={}", REPOSITORY_URL);
        log.info("javadochub internal repository. url={}", INTERNAL_URL);
        log.info("javadochub internal group. prefix={}", INTERNAL_GROUP_PREFIX);
    }

}
