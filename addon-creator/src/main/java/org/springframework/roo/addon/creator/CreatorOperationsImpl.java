package org.springframework.roo.addon.creator;

import static java.io.File.separatorChar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.springframework.roo.url.stream.UrlInputStreamService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link CreatorOperations}.
 * 
 * @author Stefan Schmidt
 * @author Juan Carlos García
 * @since 1.1
 */
@Component
@Service
public class CreatorOperationsImpl implements CreatorOperations {

    /**
     * The types of project that can be created
     * and some project utilities
     */
    private enum Type {

        /**
         * A simple addon
         */
        SIMPLE,
        
        /**
         * A simple addon as Child Project
         */
        SIMPLECHILD,

        /**
         * An advanced addon
         */
        ADVANCED,
        
        /**
         * An advanced addon as Child project
         */
        ADVANCEDCHILD,
        
        /**
         * Parent Project
         */
        PARENT,
        
        /**
         * Osgi Bundles project
         */
        OSGIBUNDLES,
        
        /**
         * Roo Addon Suite
         */
        SUITE,
        
        /**
         * Roo Addon Suite Repository project
         */
        REPOSITORY,
        
        /**
         * A language bundle
         */
        I18N,

        /**
         * An OSGi wrapper for a non-OSGi library
         */
        WRAPPER
    };

    private static final String ICON_SET_URL = "http://www.famfamfam.com/lab/icons/flags/famfamfam_flag_icons.zip";
    private static final String POM_XML = "pom.xml";

    @Reference private FileManager fileManager;
    @Reference private PathResolver pathResolver;
    @Reference private ProjectOperations projectOperations;
    @Reference private UrlInputStreamService httpService;

    private String iconSetUrl;

    protected void activate(final ComponentContext context) {
        iconSetUrl = context.getBundleContext().getProperty(
                "creator.i18n.iconset.url");
        if (StringUtils.isBlank(iconSetUrl)) {
            iconSetUrl = ICON_SET_URL;
        }
    }
    
    public void createRooAddonSuite(final JavaPackage topLevelPackage,
            final String description, String projectName){
    	// Creating advancedAddon on Spring Roo Addon Suite
    	createAdvancedAddon(topLevelPackage, description, projectName, "addon-advanced");
    	
    	// Creating simple addon on Spring Roo Addon Suite
    	createSimpleAddon(topLevelPackage, description, projectName, "addon-simple");
    	
    	// Creating osgi-addon 
    	createProject(topLevelPackage, Type.OSGIBUNDLES, description, projectName, "osgi-bundles");
    	
    	// Creating roo-addon-suite
    	createProject(topLevelPackage, Type.SUITE, description, projectName, "roo-addon-suite");
    	
    	// Creating repository project
    	createProject(topLevelPackage, Type.REPOSITORY, description, projectName, "repository");
    	
    	// Creating parent project
    	createProject(topLevelPackage, Type.PARENT, description, projectName, null);
    	
    	// Including suite-dev file on ROOT
    	copyFile("suite-dev", null);
    }
    

    

	public void createAdvancedAddon(final JavaPackage topLevelPackage,
            final String description, final String projectName, String folder) {
        Validate.notNull(topLevelPackage, "Top-level package required");

        if(folder != null){
        	createProject(topLevelPackage, Type.ADVANCEDCHILD, description, projectName, folder);
        }else{
        	createProject(topLevelPackage, Type.ADVANCED, description, projectName, folder);
        }

        install("Commands.java", topLevelPackage, Path.SRC_MAIN_JAVA,
                Type.ADVANCED, projectName, folder);
        install("Operations.java", topLevelPackage, Path.SRC_MAIN_JAVA,
                Type.ADVANCED, projectName, folder);
        install("OperationsImpl.java", topLevelPackage, Path.SRC_MAIN_JAVA,
                Type.ADVANCED, projectName, folder);
        install("Metadata.java", topLevelPackage, Path.SRC_MAIN_JAVA,
                Type.ADVANCED, projectName, folder);
        install("MetadataProvider.java", topLevelPackage, Path.SRC_MAIN_JAVA,
                Type.ADVANCED, projectName, folder);
        install("RooAnnotation.java", topLevelPackage, Path.SRC_MAIN_JAVA,
                Type.ADVANCED, projectName, folder);
        install("configuration.xml", topLevelPackage, Path.SRC_MAIN_RESOURCES,
                Type.ADVANCED, projectName, folder);
        
        createObrFile(topLevelPackage.getFullyQualifiedPackageName(), Type.ADVANCED, folder);
    }

    
	private void createObrFile(String topLevelPackageName, Type type, String folder) {
		
		// Getting obr location
		String obrLocation;
		if(folder != null){
			obrLocation = folder + "/src/main/resources/obr.xml";			
		}else{
			obrLocation = "src/main/resources/obr.xml";
		}
    	fileManager.createFile(obrLocation);
    	
    	final InputStream templateInputStream = FileUtils.getInputStream(
                getClass(), type.name().toLowerCase() + "/obr-template.xml");
        final Document docXml = XmlUtils.readXml(templateInputStream);
        final Element document = docXml.getDocumentElement();
         
        Element capabilityElement = XmlUtils.findFirstElement(
                 "resource/capability", document);
         
         if(capabilityElement != null){
        	 
        	 if(type.equals(Type.ADVANCED)){
        		 
        		 String commandInit = topLevelPackageName.substring(
                         topLevelPackageName.lastIndexOf(".") + 1)
                         .toLowerCase();
        		 
        		 Element commandAddElement = docXml.createElement("p");
        		 commandAddElement.setAttribute("n", "command-add");
        		 commandAddElement.setAttribute("v", commandInit + " add");
        		 
        		 Element commandAllElement = docXml.createElement("p");
        		 commandAllElement.setAttribute("n", "command-all");
        		 commandAllElement.setAttribute("v", commandInit + " all");
        		 
        		 Element commandSetupElement = docXml.createElement("p");
        		 commandSetupElement.setAttribute("n", "command-setup");
        		 commandSetupElement.setAttribute("v", commandInit + " setup");
        		 
        		 
        		 capabilityElement.appendChild(commandAddElement);
        		 capabilityElement.appendChild(commandAllElement);
        		 capabilityElement.appendChild(commandSetupElement);
        		 
        	 }
        	 
        	 
        	 if(type.equals(Type.SIMPLE)){
        		 
        		 Element commandSayHelloElement = docXml.createElement("p");
        		 commandSayHelloElement.setAttribute("n", "command-say-hello");
        		 commandSayHelloElement.setAttribute("v", "say hello");
        		 
        		 Element commandInstallElement = docXml.createElement("p");
        		 commandInstallElement.setAttribute("n", "command-install-tags");
        		 commandInstallElement.setAttribute("v", "web mvc install tags");
        		 
        		 capabilityElement.appendChild(commandSayHelloElement);
        		 capabilityElement.appendChild(commandInstallElement);

        	 }
        	 
        	 XmlUtils.writeXml(fileManager.updateFile(obrLocation)
                     .getOutputStream(), docXml);
         }
		
	}

	public void createI18nAddon(final JavaPackage topLevelPackage,
            String language, final Locale locale, final File messageBundle,
            final File flagGraphic, String description, final String projectName) {
        Validate.notNull(topLevelPackage, "Top Level Package required");
        Validate.notNull(locale, "Locale required");
        Validate.notNull(messageBundle, "Message Bundle required");

        if (StringUtils.isBlank(language)) {
            language = "";
            final InputStream inputStream = FileUtils
                    .getInputStream(getClass(), Type.I18N.name().toLowerCase()
                            + "/iso3166.txt");
            try {
                for (String line : IOUtils.readLines(inputStream)) {
                    final String[] split = line.split(";");
                    if (split[1].startsWith(locale.getCountry().toUpperCase())) {
                        if (split[0].contains(",")) {
                            split[0] = split[0].substring(0,
                                    split[0].indexOf(",") - 1);
                        }
                        final String[] langWords = split[0].split("\\s");
                        final StringBuilder b = new StringBuilder();
                        for (final String word : langWords) {
                            b.append(StringUtils.capitalize(word.toLowerCase()))
                                    .append(" ");
                        }
                        language = b.toString().substring(0, b.length() - 1);
                    }
                }
            }
            catch (final IOException e) {
                throw new IllegalStateException(
                        "Could not parse ISO 3166 language list, please use --language option in command");
            }
            finally {
                IOUtils.closeQuietly(inputStream);
            }
        }

        final String[] langWords = language.split("\\s");
        final StringBuilder builder = new StringBuilder();
        for (final String word : langWords) {
            builder.append(StringUtils.capitalize(word.toLowerCase()));
        }
        final String languageName = builder.toString();
        final String packagePath = topLevelPackage
                .getFullyQualifiedPackageName().replace('.', separatorChar);

        if (StringUtils.isBlank(description)) {
            description = languageName
                    + " language support for Spring Roo Web MVC JSP Scaffolding";
        }
        if (!description.contains("#mvc")
                || !description.contains("#localization")
                || !description.contains("locale:")) {
            description = description + "; #mvc,#localization,locale:"
                    + locale.getCountry().toLowerCase();
        }
        createProject(topLevelPackage, Type.I18N, description, projectName, null);

        OutputStream outputStream = null;
        try {
            outputStream = fileManager.createFile(
                    pathResolver.getFocusedIdentifier(
                            Path.SRC_MAIN_RESOURCES,
                            packagePath + separatorChar
                                    + messageBundle.getName()))
                    .getOutputStream();
            org.apache.commons.io.FileUtils.copyFile(messageBundle,
                    outputStream);
            if (flagGraphic != null) {
                outputStream = fileManager
                        .createFile(
                                pathResolver.getFocusedIdentifier(
                                        Path.SRC_MAIN_RESOURCES,
                                        packagePath + separatorChar
                                                + flagGraphic.getName()))
                        .getOutputStream();
                org.apache.commons.io.FileUtils.copyFile(flagGraphic,
                        outputStream);
            }
            else {
                installFlagGraphic(locale, packagePath);
            }
        }
        catch (final IOException e) {
            throw new IllegalStateException(
                    "Could not copy addon resources into project", e);
        }
        finally {
            IOUtils.closeQuietly(outputStream);
        }

        final String destinationFile = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_JAVA, packagePath + separatorChar + languageName
                        + "Language.java");

        if (!fileManager.exists(destinationFile)) {
            final InputStream templateInputStream = FileUtils.getInputStream(
                    getClass(), Type.I18N.name().toLowerCase()
                            + "/Language.java-template");
            try {
                // Read template and insert the user's package
                String input = IOUtils.toString(templateInputStream);
                input = input.replace("__TOP_LEVEL_PACKAGE__",
                        topLevelPackage.getFullyQualifiedPackageName());
                input = input.replace("__APP_NAME__", languageName);
                input = input.replace("__LOCALE__", locale.getLanguage());
                input = input.replace("__LANGUAGE__",
                        StringUtils.capitalize(language));
                if (flagGraphic != null) {
                    input = input.replace("__FLAG_FILE__",
                            flagGraphic.getName());
                }
                else {
                    input = input.replace("__FLAG_FILE__", locale.getCountry()
                            .toLowerCase() + ".png");
                }
                input = input.replace("__MESSAGE_BUNDLE__",
                        messageBundle.getName());

                // Output the file for the user
                final MutableFile mutableFile = fileManager
                        .createFile(destinationFile);
                outputStream = mutableFile.getOutputStream();
                IOUtils.write(input, outputStream);
            }
            catch (final IOException ioe) {
                throw new IllegalStateException("Unable to create '"
                        + languageName + "Language.java'", ioe);
            }
            finally {
                IOUtils.closeQuietly(templateInputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }
    }

    /**
     * Creates the root files for a new project, namely the:
     * <ul>
     * <li>Maven POM</li>
     * <li>readme.txt</li>
     * <li>
     * 
     * @param topLevelPackage the top-level package of the project being created
     *            (required)
     * @param type the type of project being created (required)
     * @param description the description to put into the POM (can be blank)
     * @param projectName if blank, a sanitised version of the given top-level
     *            package is used for the project name
     * @param folder  
     */
    private void createProject(final JavaPackage topLevelPackage,
            final Type type, final String description, String projectName, String folder) {
    	
        if (StringUtils.isBlank(projectName)) {
            projectName = topLevelPackage.getFullyQualifiedPackageName()
                    .replace(".", "-");
        }

        // Load the POM template
        final InputStream templateInputStream = FileUtils.getInputStream(
                getClass(), type.name().toLowerCase() + "/roo-addon-"
                        + type.name().toLowerCase() + "-template.xml");
        final Document pom = XmlUtils.readXml(templateInputStream);
        final Element root = pom.getDocumentElement();

        // Populate it from the given inputs
        if(type.equals(Type.PARENT)){
        	XmlUtils.findRequiredElement("/project/artifactId", root)
            .setTextContent(topLevelPackage.getFullyQualifiedPackageName() + ".root");
        }else if(type.name().contains("CHILD") || type.equals(Type.SUITE)){
        	XmlUtils.findRequiredElement("/project/parent/artifactId", root)
            .setTextContent(topLevelPackage.getFullyQualifiedPackageName() + ".osgi.bundles");
        	XmlUtils.findRequiredElement("/project/artifactId", root)
            .setTextContent(topLevelPackage.getFullyQualifiedPackageName() + "." + folder);
        }else if(type.equals(Type.OSGIBUNDLES)){
        	XmlUtils.findRequiredElement("/project/parent/artifactId", root)
            .setTextContent(topLevelPackage.getFullyQualifiedPackageName() + ".root");
        	XmlUtils.findRequiredElement("/project/artifactId", root)
            .setTextContent(topLevelPackage.getFullyQualifiedPackageName() + ".osgi.bundles");
        }else if(type.equals(Type.REPOSITORY)){
        	XmlUtils.findRequiredElement("/project/parent/artifactId", root)
            .setTextContent(topLevelPackage.getFullyQualifiedPackageName() + ".root");
        	XmlUtils.findRequiredElement("/project/artifactId", root)
            .setTextContent(topLevelPackage.getFullyQualifiedPackageName() + ".repository");
        }else{
        	XmlUtils.findRequiredElement("/project/artifactId", root)
            .setTextContent(topLevelPackage.getFullyQualifiedPackageName());
        }
        if(type.name().contains("CHILD") || type.equals(Type.OSGIBUNDLES) || type.equals(Type.SUITE) || type.equals(Type.REPOSITORY)){
        	XmlUtils.findRequiredElement("/project/parent/groupId", root).setTextContent(
                    topLevelPackage.getFullyQualifiedPackageName());
        }else{
        	XmlUtils.findRequiredElement("/project/groupId", root).setTextContent(
                    topLevelPackage.getFullyQualifiedPackageName());
        }
        
    	if(folder != null){
            XmlUtils.findRequiredElement("/project/name", root).setTextContent(
                    projectName + " - " + folder);
        }else{
            XmlUtils.findRequiredElement("/project/name", root).setTextContent(
                    projectName);
            
            if (StringUtils.isNotBlank(description)) {
                XmlUtils.findRequiredElement("/project/description", root)
                        .setTextContent(description);
            }
        }
    	
        // Write the new POM to disk
        writePomFile(pom, folder);
        
       	// Updating dependencies when pom.xml
        // is generated
    	if(type.equals(Type.SUITE) || type.equals(Type.REPOSITORY)){
    		addAddonsDependencies(pom, folder, topLevelPackage);
    	}
    	
    	// Including MANIFEST.MF file only when is a Suite project
    	if(type.equals(Type.SUITE)){
    		writeTextFile("MANIFEST.MF", generateManifestContent(topLevelPackage, description, projectName), folder + "/src/main/esa/META-INF");
    	}
    	
    	// Including src/main/assembly/repo-assembly.xml when is a repository project
    	if(type.equals(Type.REPOSITORY)){
    		writeAssemblyFile(topLevelPackage, "repo-assembly-template.xml", folder);
    		copyFile("suite.css", folder);
    		copyFile("obr2html.xsl", folder);
    		copyFile("style.css", folder);
    		copyFile("bootstrap.min.css", folder);
    	}

        // Write the other root files
        writeTextFile("readme.txt", "Welcome to my addon!", folder);
        writeTextFile("legal" + separatorChar + "LICENSE.TXT",
                "Your license goes here", folder);
        fileManager.scan();
    }

    private void copyFile(String fileName, String folder) {
    	
    	String file = "";
    	if(folder != null){
    		file = folder + "/src/main/resources/" + fileName;
    	}else{
    		file = pathResolver.getFocusedIdentifier(
                    Path.ROOT, fileName);
    	}
    	
    	
    	InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = FileUtils.getInputStream(getClass(),
                    "resources/" + fileName);
            if (!fileManager.exists(file)) {
                outputStream = fileManager.createFile(file)
                        .getOutputStream();
            }
            
            if (outputStream != null) {
                IOUtils.copy(inputStream, outputStream);
            }
        }
        catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
            if (outputStream != null) {
                IOUtils.closeQuietly(outputStream);
            }

        }
    	
	}

	private void writeAssemblyFile(JavaPackage topLevelPackage, String templateName, String destinationFolder) {
    	final InputStream templateInputStream = FileUtils.getInputStream(
                getClass(), "xml/" + templateName);
    	
    	final Document assemblyDoc = XmlUtils.readXml(templateInputStream);
        final Element root = assemblyDoc.getDocumentElement();
        
        String projectFolder = topLevelPackage.getFullyQualifiedPackageName().replaceAll("\\.", "/");
        
        XmlUtils.findRequiredElement("/assembly/moduleSets/moduleSet/binaries/outputDirectory", root)
        .setTextContent(projectFolder + "/${module.artifactId}/${module.version}");
        
        // Add includes
        Element includes = XmlUtils.findFirstElement(
                "moduleSets/moduleSet/includes", root);
        
        if(includes != null){
        	
        	 // Adding addon-advanced include
        	 Element includeAdvancedElement = assemblyDoc.createElement("include");
        	 includeAdvancedElement.setTextContent(topLevelPackage.getFullyQualifiedPackageName() + ":" + topLevelPackage.getFullyQualifiedPackageName() + ".addon-advanced");
        	 
        	 // Adding addon-simple include
        	 Element includeSimpleElement = assemblyDoc.createElement("include");
        	 includeSimpleElement.setTextContent(topLevelPackage.getFullyQualifiedPackageName() + ":" + topLevelPackage.getFullyQualifiedPackageName() + ".addon-simple");
        	 
        	 // Adding roo-addon-suite include
        	 Element includeSuiteElement = assemblyDoc.createElement("include");
        	 includeSuiteElement.setTextContent(topLevelPackage.getFullyQualifiedPackageName() + ":" + topLevelPackage.getFullyQualifiedPackageName() + ".roo-addon-suite");
        	
        	 includes.appendChild(includeAdvancedElement);
        	 includes.appendChild(includeSimpleElement);
        	 includes.appendChild(includeSuiteElement);
        }
        
        MutableFile assemblyFile = fileManager.createFile("src/main/assembly/repo-assembly.xml");
        
        XmlUtils.writeXml(assemblyFile.getOutputStream(), assemblyDoc);
		
	}

	private String generateManifestContent(JavaPackage topLevelPackage, String description, String projectName) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("Manifest-Version: 1.0\n");
		sb.append("Bnd-LastModified: 1427459113830\n");
		sb.append("Build-Jdk: 1.7.0_60\n");
		sb.append("Bundle-Description: " + description + "\n");
		sb.append("Bundle-License: http://www.gnu.org/licenses/gpl-3.0.html\n");
		sb.append("Bundle-ManifestVersion: 2\n");
		sb.append("Bundle-Name: " + projectName + " - Roo Addon Suite\n");
		sb.append("Bundle-SymbolicName: " + topLevelPackage.getFullyQualifiedPackageName() + ".roo.addon.suite\n");
		sb.append("Bundle-Version: 1.0.0.BUILD-SNAPSHOT\n");
		sb.append("Created-By: Apache Maven Bundle Plugin\n");
		sb.append("Tool: Bnd-2.3.0.201405100607");
		
		return sb.toString();

	}

	private void addAddonsDependencies(Document pom, String folder, JavaPackage topLevelPackage) {
		// Getting Roo Addon Suite pom.xml file
    	String pomRooAddonSuite = projectOperations.getPathResolver()
                .getIdentifier(Path.ROOT.getModulePathId(""), folder + "/pom.xml");
    	
    	Validate.isTrue(fileManager.exists(pomRooAddonSuite),
                folder + "/pom.xml not found");
    	
    	InputStream inputStream = fileManager.getInputStream(pomRooAddonSuite);
    	
    	 Document docXml = XmlUtils.readXml(inputStream);
    	 
    	// Getting root element
         Element document = docXml.getDocumentElement();
         
         Element dependenciesElement = XmlUtils.findFirstElement(
                 "dependencies", document);
         
         if(dependenciesElement != null){
        	 
        	 // Adding addon-advanced dependencies
        	 Element dependencyAdvancedElement = docXml.createElement("dependency");
        	 Element groupIdAdvancedElement = docXml.createElement("groupId");
        	 groupIdAdvancedElement.setTextContent(topLevelPackage.getFullyQualifiedPackageName());
        	 Element artifactIdAdvancedElement = docXml.createElement("artifactId");
        	 artifactIdAdvancedElement.setTextContent(topLevelPackage.getFullyQualifiedPackageName() + ".addon-advanced");
        	 Element versionAdvancedElement = docXml.createElement("version");
        	 versionAdvancedElement.setTextContent("${project.parent.version}");
        	 dependencyAdvancedElement.appendChild(groupIdAdvancedElement);
        	 dependencyAdvancedElement.appendChild(artifactIdAdvancedElement);
        	 dependencyAdvancedElement.appendChild(versionAdvancedElement);
        	 
        	 // Adding addon-simple dependencies
        	 Element dependencyElement = docXml.createElement("dependency");
        	 Element groupIdElement = docXml.createElement("groupId");
        	 groupIdElement.setTextContent(topLevelPackage.getFullyQualifiedPackageName());
        	 Element artifactIdElement = docXml.createElement("artifactId");
        	 artifactIdElement.setTextContent(topLevelPackage.getFullyQualifiedPackageName() + ".addon-simple");
        	 Element versionElement = docXml.createElement("version");
        	 versionElement.setTextContent("${project.parent.version}");
        	 dependencyElement.appendChild(groupIdElement);
        	 dependencyElement.appendChild(artifactIdElement);
        	 dependencyElement.appendChild(versionElement);
        	 
        	 dependenciesElement.appendChild(dependencyElement);
        	 dependenciesElement.appendChild(dependencyAdvancedElement);
        	 
        	 XmlUtils.writeXml(fileManager.updateFile(pomRooAddonSuite)
                     .getOutputStream(), docXml);
        	 
         }
		
	}

	public void createSimpleAddon(final JavaPackage topLevelPackage,
            final String description, final String projectName, String folder) {
        Validate.notNull(topLevelPackage, "Top Level Package required");

        if(folder != null){
        	createProject(topLevelPackage, Type.SIMPLECHILD, description, projectName, folder);
        }else{
        	createProject(topLevelPackage, Type.SIMPLE, description, projectName, folder);
        }

        install("Commands.java", topLevelPackage, Path.SRC_MAIN_JAVA,
                Type.SIMPLE, projectName, folder);
        install("Operations.java", topLevelPackage, Path.SRC_MAIN_JAVA,
                Type.SIMPLE, projectName, folder);
        install("OperationsImpl.java", topLevelPackage, Path.SRC_MAIN_JAVA,
                Type.SIMPLE, projectName, folder);
        install("PropertyName.java", topLevelPackage, Path.SRC_MAIN_JAVA,
                Type.SIMPLE, projectName, folder);
        install("info.tagx", topLevelPackage, Path.SRC_MAIN_RESOURCES,
                Type.SIMPLE, projectName, folder);
        install("show.tagx", topLevelPackage, Path.SRC_MAIN_RESOURCES,
                Type.SIMPLE, projectName, folder);
        
        createObrFile(topLevelPackage.getFullyQualifiedPackageName(), Type.SIMPLE, folder);
    }

    public void createWrapperAddon(final JavaPackage topLevelPackage,
            final String groupId, final String artifactId,
            final String version, final String vendorName,
            final String lincenseUrl, final String docUrl,
            final String osgiImports, final String description,
            String projectName) {
        Validate.notNull(topLevelPackage, "Top Level Package required");
        if (StringUtils.isBlank(projectName)) {
            projectName = topLevelPackage.getFullyQualifiedPackageName()
                    .replace(".", "-");
        }
        final String wrapperGroupId = topLevelPackage
                .getFullyQualifiedPackageName();

        final InputStream templateInputStream = FileUtils.getInputStream(
                getClass(), "wrapper/roo-addon-wrapper-template.xml");
        final Document pom = XmlUtils.readXml(templateInputStream);
        final Element root = pom.getDocumentElement();

        XmlUtils.findRequiredElement("/project/name", root).setTextContent(
                projectName);
        XmlUtils.findRequiredElement("/project/groupId", root).setTextContent(
                wrapperGroupId);
        XmlUtils.findRequiredElement("/project/artifactId", root)
                .setTextContent(wrapperGroupId + "." + artifactId);
        XmlUtils.findRequiredElement("/project/version", root).setTextContent(
                version + ".0001");
        XmlUtils.findRequiredElement(
                "/project/dependencies/dependency/groupId", root)
                .setTextContent(groupId);
        XmlUtils.findRequiredElement(
                "/project/dependencies/dependency/artifactId", root)
                .setTextContent(artifactId);
        XmlUtils.findRequiredElement(
                "/project/dependencies/dependency/version", root)
                .setTextContent(version);
        XmlUtils.findRequiredElement("/project/properties/pkgArtifactId", root)
                .setTextContent(artifactId);
        XmlUtils.findRequiredElement("/project/properties/pkgVersion", root)
                .setTextContent(version);
        XmlUtils.findRequiredElement("/project/properties/pkgVendor", root)
                .setTextContent(vendorName);
        XmlUtils.findRequiredElement("/project/properties/pkgLicense", root)
                .setTextContent(lincenseUrl);
        XmlUtils.findRequiredElement("/project/properties/repo.folder", root)
                .setTextContent(
                        topLevelPackage.getFullyQualifiedPackageName().replace(
                                ".", "/"));
        if (docUrl != null && docUrl.length() > 0) {
            XmlUtils.findRequiredElement("/project/properties/pkgDocUrl", root)
                    .setTextContent(docUrl);
        }
        if (osgiImports != null && osgiImports.length() > 0) {
            final Element config = XmlUtils
                    .findRequiredElement(
                            "/project/build/plugins/plugin[artifactId = 'maven-bundle-plugin']/configuration/instructions",
                            root);
            config.appendChild(new XmlElementBuilder("Import-Package", pom)
                    .setText(osgiImports).build());
        }
        if (description != null && description.length() > 0) {
            final Element descriptionE = XmlUtils.findRequiredElement(
                    "/project/description", root);
            descriptionE.setTextContent(description + " "
                    + descriptionE.getTextContent());
        }

        writePomFile(pom, null);
    }

    private String getErrorMsg(final String localeStr) {
        return "Could not acquire flag icon for locale " + localeStr
                + " please use --flagGraphic to specify the flag manually";
    }

    private void install(final String targetFilename,
            final JavaPackage topLevelPackage, final Path path,
            final Type type, String projectName, String folder) {
        if (StringUtils.isBlank(projectName)) {
            projectName = topLevelPackage.getFullyQualifiedPackageName()
                    .replace(".", "-");
        }
        final String topLevelPackageName = topLevelPackage
                .getFullyQualifiedPackageName();
        final String packagePath = topLevelPackageName.replace('.',
                separatorChar);
        String destinationFile = "";

        if (targetFilename.endsWith(".java")) {
        	
        	if(folder != null){
        		destinationFile =  folder + "/" + path.getDefaultLocation() + "/" + packagePath + separatorChar
                        + StringUtils.capitalize(topLevelPackageName
                                .substring(topLevelPackageName
                                        .lastIndexOf(".") + 1))
                        + targetFilename;
        	}else{
        		destinationFile =  path.getDefaultLocation() + "/" + packagePath + separatorChar
                        + StringUtils.capitalize(topLevelPackageName
                                .substring(topLevelPackageName
                                        .lastIndexOf(".") + 1))
                        + targetFilename;
        	}
        }
        else {
        	if(folder != null){
        		destinationFile =  folder + "/" + path.getDefaultLocation() + "/" + packagePath + separatorChar + targetFilename;
        	}else{
        		destinationFile =  path.getDefaultLocation() + "/" + packagePath + separatorChar + targetFilename;
        	}
        }

        // Adjust name for Roo Annotation
        if (targetFilename.startsWith("RooAnnotation")) {
        	
        	if(folder != null){
        		destinationFile =  folder + "/" + path.getDefaultLocation() + "/" + packagePath + separatorChar + "Roo"
                        + StringUtils.capitalize(topLevelPackageName
                                .substring(topLevelPackageName
                                        .lastIndexOf(".") + 1)) + ".java";
        	}else{
        		destinationFile =  path.getDefaultLocation() + "/" + packagePath + separatorChar + "Roo"
                        + StringUtils.capitalize(topLevelPackageName
                                .substring(topLevelPackageName
                                        .lastIndexOf(".") + 1)) + ".java";
        	}
        }

        if (!fileManager.exists(destinationFile)) {
            final InputStream templateInputStream = FileUtils.getInputStream(
                    getClass(), type.name().toLowerCase() + "/"
                            + targetFilename + "-template");
            OutputStream outputStream = null;
            try {
                // Read template and insert the user's package
                String input = IOUtils.toString(templateInputStream);
                input = input.replace("__TOP_LEVEL_PACKAGE__",
                        topLevelPackage.getFullyQualifiedPackageName());
                input = input
                        .replace("__APP_NAME__", StringUtils
                                .capitalize(topLevelPackageName
                                        .substring(topLevelPackageName
                                                .lastIndexOf(".") + 1)));
                input = input.replace(
                        "__APP_NAME_LWR_CASE__",
                        topLevelPackageName.substring(
                                topLevelPackageName.lastIndexOf(".") + 1)
                                .toLowerCase());
                input = input.replace("__PROJECT_NAME__",
                        projectName.toLowerCase());

                // Output the file for the user
                final MutableFile mutableFile = fileManager
                        .createFile(destinationFile);
                outputStream = mutableFile.getOutputStream();
                IOUtils.write(input, outputStream);
            }
            catch (final IOException ioe) {
                throw new IllegalStateException("Unable to create '"
                        + targetFilename + "'", ioe);
            }
            finally {
                IOUtils.closeQuietly(templateInputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }
    }

    private void installFlagGraphic(final Locale locale,
            final String packagePath) {
        boolean success = false;
        final String countryCode = locale.getCountry().toLowerCase();

        // Retrieve the icon file:
        BufferedInputStream bis = null;
        ZipInputStream zis = null;
        try {
            bis = new BufferedInputStream(httpService.openConnection(new URL(
                    iconSetUrl)));
            zis = new ZipInputStream(bis);
            ZipEntry entry;
            final String expectedEntryName = "png/" + countryCode + ".png";
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(expectedEntryName)) {
                    final MutableFile target = fileManager
                            .createFile(pathResolver.getFocusedIdentifier(
                                    Path.SRC_MAIN_RESOURCES, packagePath + "/"
                                            + countryCode + ".png"));
                    OutputStream outputStream = null;
                    try {
                        outputStream = target.getOutputStream();
                        IOUtils.copy(zis, outputStream);
                        success = true;
                    }
                    finally {
                        IOUtils.closeQuietly(outputStream);
                    }
                }
            }
        }
        catch (final IOException e) {
            throw new IllegalStateException(getErrorMsg(locale.getCountry()), e);
        }
        finally {
            IOUtils.closeQuietly(bis);
            IOUtils.closeQuietly(zis);
        }

        if (!success) {
            throw new IllegalStateException(getErrorMsg(locale.toString()));
        }
    }

    public boolean isAddonCreatePossible() {
        return !projectOperations.isFocusedProjectAvailable();
    }

    /**
     * Writes the given Maven POM to disk
     * 
     * @param pom the POM to write (required)
     */
    private void writePomFile(final Document pom, String folder) {
    	MutableFile pomFile;
    	if(folder != null){
    		pomFile = fileManager.createFile(folder + "/" + POM_XML);
    	}else{
    		LogicalPath rootPath = LogicalPath.getInstance(Path.ROOT, "");
    		pomFile = fileManager.createFile(pathResolver
                    .getIdentifier(rootPath, POM_XML));
    	}
        
        XmlUtils.writeXml(pomFile.getOutputStream(), pom);
    }

    private void writeTextFile(final String fullPathFromRoot,
            final String message, String folder) {
        Validate.notBlank(fullPathFromRoot,
                "Text file name to write is required");
        Validate.notBlank(message, "Message required");
        
        MutableFile mutableFile;
        if(folder != null){
        	mutableFile = fileManager.createFile(folder + "/" + fullPathFromRoot);
        }else{
        	String path = pathResolver.getFocusedIdentifier(Path.ROOT,
                    fullPathFromRoot);
        	mutableFile = fileManager.exists(path) ? fileManager
                    .updateFile(path) : fileManager.createFile(path);
        }
        
        OutputStream outputStream = null;
        try {
            outputStream = mutableFile.getOutputStream();
            IOUtils.write(message, outputStream);
        }
        catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
        finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

}
