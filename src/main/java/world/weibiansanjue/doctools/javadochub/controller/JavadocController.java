package world.weibiansanjue.doctools.javadochub.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import world.weibiansanjue.doctools.javadochub.repository.IRepository;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * javadoc 获取控制器.
 *
 * @author weibiansanjue
 * @since 0.1.0
 */
@Slf4j
@Controller
public class JavadocController {

    private static final String URL_GA   = "/{rep:doc|snapshot}/{groupId}/{artifactId}";
    private static final String URL_PAGE = "/{rep:doc|snapshot}/{groupId}/{artifactId}/{version}/**";

    private static final String INDEX_PAGE = "overview-summary.html";

    @Value("${javadochub.version}")
    private String JAVADOCHUB_VERSION;

    @Resource
    @Qualifier("repository")
    private IRepository repositoryRelease;

    @Resource
    private IRepository repositorySnapshot;

    private IRepository repository;


    @GetMapping(value = "/{rep:doc|snapshot}")
    public String redirectHelp() {
        return "redirect:/";
    }

    /**
     * 通过 groupId, artifactId 获取 javadoc.
     *
     * @author weibiansanjue
     * @param modelView mode view
     * @param groupId group id
     * @param artifactId aritfact id
     * @return doc.html
     * @throws IOException io exception
     * @since 0.1.0
     */
    @GetMapping(value = URL_GA)
    public ModelAndView getJavadocUrl(ModelAndView modelView,
                                      @PathVariable("rep")        String rep,
                                      @PathVariable("groupId")    String groupId,
                                      @PathVariable("artifactId") String artifactId) throws IOException {
        repository = getRepository(groupId, rep);
        extract(modelView, groupId, artifactId, null, INDEX_PAGE, resetRep(groupId, rep));
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
     * @since 0.1.0
     */
    @GetMapping(value = URL_PAGE)
    public ModelAndView getJavadocUrl(ModelAndView modelView,
                                      @PathVariable("rep")        String rep,
                                      @PathVariable("groupId")    String groupId,
                                      @PathVariable("artifactId") String artifactId,
                                      @PathVariable("version")    String version,
                                      HttpServletRequest          request) throws IOException {


        repository = getRepository(groupId, rep);
        String page = new AntPathMatcher().extractPathWithinPattern(URL_PAGE, request.getServletPath());
        page = StringUtils.isEmpty(page) ? INDEX_PAGE : page;
        extract(modelView, groupId, artifactId, version, page, resetRep(groupId, rep));

        return modelView;
    }

    private IRepository getRepository(String groupId, String rep) {
        if ("snapshot".equals(rep) && repositorySnapshot.isInternal(groupId)) {
            return repositorySnapshot;
        }
        return repositoryRelease;
    }

    private String resetRep(String groupId, String rep) {
        if ("snapshot".equals(rep) && repositorySnapshot.isInternal(groupId)){
            return rep;
        }
        return "doc";
    }

    private void extract(ModelAndView modelView,
        String groupId, String artifactId, String version, String page, String rep) throws IOException {

        log.info("extract javadoc. g={} a={} v={} p={} rep={}", groupId, artifactId, version, page, rep);

        modelView.addObject("appversion", "v" + JAVADOCHUB_VERSION)
            .addObject("groupId", groupId)
            .addObject("artifactId", artifactId)
            .addObject("artifactIds", repository.artifact(groupId))
            .addObject("page", page)
            .addObject("isInternal", repository.isInternal(groupId))
            .setViewName(rep);

        Versioning versioning = repository.version(groupId, artifactId);
        if (null == versioning) {
            modelView.addObject("version", null == version ? "latest" : version)
                .addObject("status", 404);
            return;
        }
        version = StringUtils.isEmpty(version) || "latest".equals(version) ? versioning.getRelease() : version;
        modelView.addObject("version", version)
            .addObject("versions", versioning.getVersions());
        if ("sync".equals(page)) {
            repository.delete(groupId, artifactId, version);
            modelView.addObject("page", INDEX_PAGE);
        }
        int status = repository.store(groupId, artifactId, version);
        modelView.addObject("status", status)
            .addObject("download", repository.javadocDownloadUrl(groupId, artifactId, version));
    }

}
