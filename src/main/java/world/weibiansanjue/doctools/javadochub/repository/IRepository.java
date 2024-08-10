package world.weibiansanjue.doctools.javadochub.repository;

import org.apache.maven.artifact.repository.metadata.Versioning;

import java.io.IOException;
import java.util.List;

/**
 * 仓库操作.
 *
 * @author weibiansanjue
 * @since 0.3.0
 */
public interface IRepository {


    /**
     * 获取本地 artifact 名词集合.
     *
     * @author weibiansanjue
     * @param groupId group id
     * @return artifact name list
     * @since 0.1.0
     */
    List<String> artifact(String groupId);

    /**
     * 获取 maven 仓库中 javadoc 版本.
     *
     * @author weibiansanjue
     * @param groupId group id
     * @param artifactId artifact id
     * @return remote metadata
     * @since 0.1.0
     */
    Versioning version(String groupId, String artifactId);

    /**
     * 获取 maven 仓库中 javadoc 快照版本.
     *
     * @author weibiansanjue
     * @param groupId group id
     * @param artifactId artifact id
     * @param version version
     * @return remote metadata
     * @since 0.3.0
     */
    Versioning version(String groupId, String artifactId, String version);

    /**
     * metadata url.
     *
     * @author weibiansanjue
     * @param groupId group id
     * @param artifactId artifact id
     * @return remote metadata url
     * @since 0.2.0
     */
    String metadataUrl(String groupId, String artifactId);

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
    boolean delete(String groupId, String artifactId, String version) throws IOException;

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
    int store(String groupId, String artifactId, String version) throws IOException;

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
    String javadocDownloadUrl(String groupId, String artifactId, String version);

    /**
     * is internal package
     *
     * @author weibiansanjue
     * @param groupId group Id
     * @return is internal package
     * @since 0.3.0
     */
    Boolean isInternal(String groupId);

}
