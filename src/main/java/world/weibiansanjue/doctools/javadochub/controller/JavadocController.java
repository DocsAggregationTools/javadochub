package world.weibiansanjue.doctools.javadochub.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import world.weibiansanjue.doctools.javadochub.repository.Repository;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * javadoc 获取.
 *
 * @author weibiansanjue
 * @since 1.0.0
 */
@Slf4j
@Controller
public class JavadocController {

    private static final String URL_GA   = "/doc/{groupId}/{artifactId}";
    private static final String URL_PAGE = "/doc/{groupId}/{artifactId}/{version}/**";

    @Autowired
    private Repository repository;

    /**
     * 通过 groupId, artifactId 获取 javadoc.
     *
     * @author weibiansanjue
     * @param modelView mode view
     * @param groupId group id
     * @param artifactId aritfact id
     * @return doc.html
     * @throws IOException io exception
     * @since 1.0.0
     */
    @GetMapping(value = URL_GA)
    public ModelAndView getJavadocUrl(ModelAndView modelView,
                                      @PathVariable("groupId")    String groupId,
                                      @PathVariable("artifactId") String artifactId) throws IOException {

        extract(modelView, groupId, artifactId, null, "overview-summary.html");

        return modelView;
    }

    /**
     * 通过 grouptId, artifactId, version 获取 javadoc.
     *
     * @author weibiansanjue
     * @param modelView model view
     * @param groupId group id
     * @param artifactId artifact id
     * @param version version
     * @param request http request
     * @return doc.html
     * @throws IOException io exception
     * @since 1.0.0
     */
    @GetMapping(value = URL_PAGE)
    public ModelAndView getJavadocUrl(ModelAndView modelView,
                                      @PathVariable("groupId")    String groupId,
                                      @PathVariable("artifactId") String artifactId,
                                      @PathVariable("version")    String version,
                                      HttpServletRequest          request) throws IOException {

        String page = new AntPathMatcher().extractPathWithinPattern(URL_PAGE, request.getServletPath());
        page = StringUtils.isEmpty(page) ? "overview-summary.html" : page;
        extract(modelView, groupId, artifactId, version, page);

        return modelView;
    }

    private void extract(ModelAndView modelView,
        String groupId, String artifactId, String version, String page) throws IOException {

        log.info("extract javadoc. g={} a={} v={} p={}", groupId, artifactId, version, page);

        Versioning versioning = repository.version(groupId, artifactId);
        version = StringUtils.isEmpty(version) ? versioning.getRelease() : version;

        modelView.addObject("groupId", groupId)
                 .addObject("artifactId", artifactId)
                 .addObject("artifactIds", repository.artifact(groupId))
                 .addObject("version", version)
                 .addObject("versions", versioning.getVersions())
                 .addObject("page", page)
                 .setViewName("doc");

        repository.store(groupId, artifactId, version);

    }

}
