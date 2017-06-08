/**
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apereo.portal.portlets.dynamicskin.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.portlet.PortletContext;
import javax.portlet.PortletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apereo.portal.portlets.dynamicskin.DynamicRespondrSkinConstants;
import org.apereo.portal.portlets.dynamicskin.DynamicSkinException;
import org.apereo.portal.portlets.dynamicskin.DynamicSkinInstanceData;
import org.apereo.portal.portlets.dynamicskin.DynamicSkinUniqueTokenGenerator;
import org.lesscss.LessCompiler;
import org.lesscss.LessException;
import org.lesscss.LessSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Abstract base class for {@link DynamicSkinService} classes.
 */
public abstract class AbstractDynamicSkinService implements DynamicSkinService {

    protected static final String LESS_CSS_JAVASCRIPT_URL = "/media/skins/common/javascript/less/less-1.6.2.js";
    protected static final String DYNASKIN_TEMPLATE_INCLUDE_FILE = "{0}/{1}.less";
    protected static final String DYNASKIN_INCLUDE_FILE = "{0}/configuredSkin-{1}.less";
    protected static final String DYNAMIC_SKIN_FILENAME_BASE = "skin";

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected MessageFormat skinTemplateIncludeFile = new MessageFormat(DYNASKIN_TEMPLATE_INCLUDE_FILE);
    protected MessageFormat skinIncludeFile = new MessageFormat(DYNASKIN_INCLUDE_FILE);

    protected String localRelativeRootPath = DynamicRespondrSkinConstants.DEFAULT_RELATIVE_ROOT_FOLDER;
    protected String lessCssJavascriptUrlPath = LESS_CSS_JAVASCRIPT_URL;

    /**
     *  Set of CSS instance keys for skin files that currently exist.  Thread-safe for concurrent reads and inserts.
     */
    protected Set<String> instanceKeysForExistingCss = new CopyOnWriteArraySet<String>();

    protected Cache cssSkinFailureCache;
    private DynamicSkinUniqueTokenGenerator uniqueTokenGenerator;
    private DynamicSkinCssFileNamer cssFileNamer;

    public AbstractDynamicSkinService(
            final DynamicSkinUniqueTokenGenerator uniqueTokenGenerator,
            final DynamicSkinCssFileNamer namer,
            final Cache failureCache) {
        Assert.notNull(failureCache);
        Assert.notNull(uniqueTokenGenerator);
        Assert.notNull(namer);
        this.cssSkinFailureCache = failureCache;
        this.uniqueTokenGenerator = uniqueTokenGenerator;
        this.cssFileNamer = namer;
    }



    @Override
    abstract public String getSkinCssPath(DynamicSkinInstanceData data);

    public String getSkinLessTemplatePath(DynamicSkinInstanceData data) {
        final String templateRelativePath =
                this.skinTemplateIncludeFile.format(new Object[] {this.localRelativeRootPath, data.getSkinName()});
        return data.getPortletAbsolutePathRoot() + templateRelativePath;
    }

    public String getSkinLessPath(DynamicSkinInstanceData data) {
        final String includeRelativePath =
                this.skinIncludeFile.format(new Object[] {this.localRelativeRootPath, this.getUniqueToken(data)});
        return data.getPortletAbsolutePathRoot() + includeRelativePath;
    }

    /**
     * Return true if the skin file already exists.  Check memory first in a concurrent manner to allow multiple 
     * threads to check simultaneously.
     *
     * @param filePathname Fully-qualified file path name of the .css file
     * @return True if file exists on the file system.
     */
    @Override
    public boolean skinCssFileExists(DynamicSkinInstanceData data) {
        final String cssInstanceKey = this.getCssInstanceKey(data);
        // Check the existing map first since it is faster than accessing the actual file.
        if (this.instanceKeysForExistingCss.contains(cssInstanceKey)) {
            return true;
        }
        boolean exists = this.innerSkinCssFileExists(data);
        if (exists) {
            if (!this.supportsRetainmentOfNonCurrentCss()) {
                this.instanceKeysForExistingCss.clear();
            }
            this.instanceKeysForExistingCss.add(cssInstanceKey);
        }
        return exists;
    }

    protected String getCssInstanceKey(DynamicSkinInstanceData data) {
        return data.getSkinName() + this.getUniqueToken(data);
    }

    /**
     * Methods that subclasses should define to return true if they support retaining of non-current CSS files (meaning 
     * that when changes are made both the old and the new compiled CSS are accessible), or false if only a single CSS 
     * with the latest updates is accessible.  Returning true would prevent recompilation if, for example, some skin 
     * changes are made but then are changed back to the original values.  If this method returns true, then that 
     * requires the subclass to be able to map CSS instance keys to the proper corresponding CSS files.
     * 
     * @return true if retainment of old CSS files is supported; false otherwise
     */
    abstract protected boolean supportsRetainmentOfNonCurrentCss();

    /**
     * Method that subclasses should define to do expensive check to see if the skin CSS file exists. This method 
     * should differ from {@link #skinFileExists(DynamicSkinInstanceData)} in that it does actual check for the 
     * existence of the file on every request versus only when an in-memory cached marker value is not found.
     * 
     * @see #compiledCssFilepaths
     * @param data skin instance data
     * @return true if css file exists, false otherwise
     */
    abstract protected boolean innerSkinCssFileExists(DynamicSkinInstanceData data);

    /**
     * Creates the skin css file in a thread-safe manner that allows multiple different skin files to be created
     * simultaneously to handle large tenant situations where all the custom CSS files were cleared away after a
     * uPortal deploy.
     *
     * Since the less compilation phase is fairly slow (several seconds) and intensive, this method will
     * allow multiple threads to process different less compilations at the same time but ensure the same
     * output file will not be created multiple times. Also this method will not let a bad LESS file cause repeated
     * LESS compilations and completely take down the portal.  The bad file will be blacklisted for a period
     * of time to limit performance impacts.
     * 
     * @see DynamicSkinService#generateSkinCssFile(DynamicSkinInstanceData)
     */
    @Override
    public void generateSkinCssFile(DynamicSkinInstanceData data) {
        final String cssInstanceKey = this.getCssInstanceKey(data);
        synchronized(cssInstanceKey) {
            if (this.instanceKeysForExistingCss.contains(cssInstanceKey)) {
                /*
                 * Two or more threads needing the same CSS file managed to invoke 
                 * this method.  An earlier thread has already generated the file 
                 * we need.  Concurrency features of the CopyOnWriteArraySet 
                 * (compiledCssFilepaths) guarantee that we will enter this if {} 
                 * block (and exit) for a filePathname that's been successfully 
                 * generated by another thread.
                 */
                return;
            }
            try {
                if (!this.cssSkinFailureCache.getKeysWithExpiryCheck().contains(cssInstanceKey)) {
                    this.createLessIncludeFile(data);
                    this.processLessFile(data);
                    if (!this.supportsRetainmentOfNonCurrentCss()) {
                        this.instanceKeysForExistingCss.clear();
                    }
                    this.instanceKeysForExistingCss.add(cssInstanceKey);
                } else {
                    // Though this should never happen except when developers are modifying the LESS files and make a mistake,
                    // if we previously tried to create the CSS file and failed for some reason, don't try to compile it
                    // again for a bit since the process is so processor intensive. It would virtually hang the uPortal
                    // service trying to compile a bad LESS file repeatedly on different threads.
                    log.warn("Skipping generation of CSS file {} due to previous LESS compilation failures", cssInstanceKey);
                }
            } catch (Exception e) {
                this.cssSkinFailureCache.put(new Element(cssInstanceKey, cssInstanceKey));
                throw new RuntimeException("Error compiling the LESS file to create:  " + cssInstanceKey, e);
            }
        }
    }

    /**
     * Create the less include file by appending the configurable preference definitions (minus the configuration
     * prefix string) to the end of the template file; e.g. portlet preference name
     * PREFcolor1 is written to the less file as @color1:prefValue
     * 
     * For preferences that end in "URL" or "Url", the values must be written as: url('<value>');
     * So for example, preference PREFmyImageUrl with value "http://fake.site/images/blah.png" would be written to the
     * less file as @myImageUrl: url('http://fake.site/images/blah.png');
     * 
     * @param prefs Portlet preferences
     * @param filename name of the less include file to create
     * @param templateFile template less include file
     * @throws IOException
     */
    protected void createLessIncludeFile(DynamicSkinInstanceData data) throws IOException {
        // Create a set of less variable assignments.
        final StringBuilder str = new StringBuilder();
        for (Entry<String, String> entry : data.getVariableNameToValueMap().entrySet()) {
            this.appendPrefAsVariable(str, entry.getKey(), entry.getValue());
        }

        // Create byte[]s of the template and preferences content
        byte[] prefsContent = str.toString().getBytes();
        File f = new File(this.getSkinLessTemplatePath(data));
        byte[] templateContent = IOUtils.toByteArray(f.toURI());

        // Create a less include file by appending the less variable definitions to the end of the template less
        // include file.  Insure there is a newline at the end of the template content or the first preference
        // value will be lost.
        byte[] newline = "\n".getBytes();
        byte[] fileContent = new byte[templateContent.length + newline.length + prefsContent.length];
        System.arraycopy(templateContent, 0, fileContent, 0, templateContent.length);
        System.arraycopy(newline, 0, fileContent, templateContent.length, newline.length);
        System.arraycopy(prefsContent, 0, fileContent, templateContent.length + newline.length, prefsContent.length);
        File lessInclude = new File(this.getSkinLessPath(data));
        IOUtils.write(fileContent, new FileOutputStream(lessInclude));
    }

    private void appendPrefAsVariable(final StringBuilder str, final String name, final String value) {
        if (StringUtils.isBlank(value)) {
            log.warn("Dynamic Skin Variable \"{}\" is not set", name);
        } else {
            str.append("@").append(name).append(": ").append(value).append(";\n");
        }
    }

    /**
     * Less compile the include file into a temporary css file.  When done rename the temporary css file to the
     * correct output filename.  Since the less compilation phase takes several seconds, this insures the
     * output css file is does not exist on the filesystem until it is complete.
     * 
     * @param lessIncludeFilepath less include file that includes all dependencies
     * @param outputFilepath name of the output css file
     * @param lessCssJavascriptUrl lessCssJavascript compiler url
     * @throws IOException
     * @throws LessException
     */
    private void processLessFile(DynamicSkinInstanceData data) throws IOException, LessException {
        final PortletContext ctx = data.getPortletRequest().getPortletSession().getPortletContext();
        final URL lessCssJavascriptUrl = ctx.getResource(this.lessCssJavascriptUrlPath);
        final LessSource lessSource = new LessSource(new File(this.getSkinLessPath(data)));
        if (log.isDebugEnabled()) {
            final String result = lessSource.getNormalizedContent();
            final File lessSourceOutput = new File(this.getSkinCssTempFileAbsolutePath(data) + "lesssource");
            IOUtils.write(result, new FileOutputStream(lessSourceOutput));
            log.debug(
                    "Full Less source from include file {0}, using lessCssJavascript at {1}"
                    + ", is at {2}, output css will be written to {3}",
                    this.getSkinLessPath(data),
                    lessCssJavascriptUrl.toString(),
                    lessSourceOutput,
                    this.getSkinCssPath(data));
        }
        final LessCompiler compiler = new LessCompiler();
        compiler.setLessJs(lessCssJavascriptUrl);
        compiler.setCompress(true);
        final File tempOutputFile = new File(this.getSkinCssTempFileAbsolutePath(data));
        compiler.compile(lessSource, tempOutputFile);
        this.moveCssFileToFinalLocation(data, tempOutputFile);
    }

    protected String getLocalRootAbsoluteFilepath(DynamicSkinInstanceData data) {
         return data.getPortletAbsolutePathRoot() + this.localRelativeRootPath;
    }

    protected String getSkinCssFilename(final DynamicSkinInstanceData data) {
        final String result = this.cssFileNamer.generateCssFileName(data);
        if (StringUtils.isBlank(result)) {
            throw new DynamicSkinException("Dynamic Skin CSS filename cannot be null or empty.");
        }
        return result;
    }

    protected String getSkinCssTempFileAbsolutePath(DynamicSkinInstanceData data) {
        return this.getLocalRootAbsoluteFilepath(data) + this.getSkinCssFilename(data);
    }

    abstract protected void moveCssFileToFinalLocation(DynamicSkinInstanceData data, final File tempCssFile);

    protected String getUniqueToken(DynamicSkinInstanceData data) {
        final String result =  this.uniqueTokenGenerator.generateToken(data);
        if (StringUtils.isBlank(result)) {
            throw new DynamicSkinException("Dynamic Skin unique token cannot be null or empty.");
        }
        return result;
    }

    @Override
    /**
     * Returns the set of skins to use.  This implementation parses the skinList.xml file and returns the set of
     * skin-key element values.  If there is an error parsing the XML file, return an empty set.
     */
    public SortedSet<String> getSkinNames(PortletRequest request) {
        // Context to access the filesystem
        PortletContext ctx = request.getPortletSession().getPortletContext();

        // Determine the full path to the skins directory
        String skinsFilepath = ctx.getRealPath(this.localRelativeRootPath + "/skinList.xml");

        // Create File object to access the filesystem
        File skinList = new File(skinsFilepath);

        TreeSet<String> skins = new TreeSet<>();
        try {
            DocumentBuilderFactory dbFactory
                    = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(skinList);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("skin-key");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                org.w3c.dom.Element element = (org.w3c.dom.Element) nList.item(temp);
                String skinName = element.getTextContent();
                log.debug("Found skin-key value {}", skinName);
                skins.add(skinName);
            }
        } catch (SAXException | ParserConfigurationException | IOException e) {
            log.error("Error processing skinsFilepath {}", skinsFilepath, e);
        }

        return skins;
    }

    @Value("${dynamic-skin.less-css-javascript-location:" + LESS_CSS_JAVASCRIPT_URL + "}")
    public void setLessCssJavascriptUrlPath(String lessCssJavascriptUrlPath) {
        this.lessCssJavascriptUrlPath = lessCssJavascriptUrlPath;
    }

    public void setLocalRelativeRootPath(String path) {
        this.localRelativeRootPath = path;
    }

    public void setUniqueTokenGenerator(final DynamicSkinUniqueTokenGenerator tokenGenerator) {
        this.uniqueTokenGenerator = tokenGenerator;
    }

    public void setSkinTemplateIncludeFile(String skinTemplateIncludeFile) {
        this.skinTemplateIncludeFile = new MessageFormat(skinTemplateIncludeFile);
    }

    public void setSkinIncludeFile(String skinIncludeFile) {
        this.skinIncludeFile = new MessageFormat(skinIncludeFile);
    }

}
