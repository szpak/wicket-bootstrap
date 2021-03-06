package de.agilecoders.wicket.samples;

import com.google.javascript.jscomp.CompilationLevel;
import de.agilecoders.wicket.Bootstrap;
import de.agilecoders.wicket.javascript.GoogleClosureJavaScriptCompressor;
import de.agilecoders.wicket.markup.html.RenderJavaScriptToFooterHeaderResponseDecorator;
import de.agilecoders.wicket.markup.html.bootstrap.extensions.html5player.Html5PlayerCssReference;
import de.agilecoders.wicket.markup.html.bootstrap.extensions.html5player.Html5PlayerJavaScriptReference;
import de.agilecoders.wicket.markup.html.bootstrap.extensions.jqueryui.JQueryUIJavaScriptReference;
import de.agilecoders.wicket.markup.html.references.BootstrapPrettifyCssReference;
import de.agilecoders.wicket.markup.html.references.BootstrapPrettifyJavaScriptReference;
import de.agilecoders.wicket.markup.html.references.ModernizrJavaScriptReference;
import de.agilecoders.wicket.markup.html.themes.metro.MetroTheme;
import de.agilecoders.wicket.samples.assets.base.ApplicationJavaScript;
import de.agilecoders.wicket.samples.assets.base.FixBootstrapStylesCssResourceReference;
import de.agilecoders.wicket.samples.pages.HomePage;
import de.agilecoders.wicket.settings.BootstrapSettings;
import de.agilecoders.wicket.settings.BootswatchThemeProvider;
import de.agilecoders.wicket.settings.ThemeProvider;
import org.apache.wicket.Application;
import org.apache.wicket.Page;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.markup.html.IPackageResourceGuard;
import org.apache.wicket.markup.html.SecurePackageResourceGuard;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.caching.FilenameWithVersionResourceCachingStrategy;
import org.apache.wicket.request.resource.caching.NoOpResourceCachingStrategy;
import org.apache.wicket.request.resource.caching.version.MessageDigestResourceVersion;
import org.apache.wicket.serialize.java.DeflatedJavaSerializer;
import org.apache.wicket.util.time.Duration;
import org.wicketstuff.annotation.scan.AnnotatedMountScanner;

import java.io.IOException;
import java.util.Properties;

/**
 * Demo Application instance.
 */
public class WicketApplication extends WebApplication {
    private Properties properties;

    /**
     * Get Application for current thread.
     *
     * @return The current thread's Application
     */
    public static WicketApplication get() {
        return (WicketApplication) Application.get();
    }

    /**
     * Constructor.
     */
    public WicketApplication() {
        super();

        properties = loadProperties();
        setConfigurationType(RuntimeConfigurationType.valueOf(properties.getProperty("configuration.type")));
    }

    /**
     * @see org.apache.wicket.Application#getHomePage()
     */
    @Override
    public Class<? extends Page> getHomePage() {
        return HomePage.class;
    }

    /**
     * @see org.apache.wicket.Application#init()
     */
    @Override
    public void init() {
        super.init();

        // wicket markup leads to strange ui problems because css selectors
        // won't match anymore.
        getMarkupSettings().setStripWicketTags(true);

        // deactivate ajax debug mode
        getDebugSettings().setAjaxDebugModeEnabled(false);

        // Allow fonts.
        IPackageResourceGuard packageResourceGuard = getResourceSettings().getPackageResourceGuard();
        if (packageResourceGuard instanceof SecurePackageResourceGuard) {
            SecurePackageResourceGuard guard = (SecurePackageResourceGuard) packageResourceGuard;
            guard.addPattern("+*.woff");
            guard.addPattern("+*.ttf");
            guard.addPattern("+*.svg");
        }

        if (usesDevelopmentConfig()) {
            getResourceSettings().setDefaultCacheDuration(Duration.NONE);
            getResourceSettings().setCachingStrategy(NoOpResourceCachingStrategy.INSTANCE);
        } else {
            getResourceSettings().setDefaultCacheDuration(Duration.days(1000));
            getResourceSettings().setCachingStrategy(new FilenameWithVersionResourceCachingStrategy(
                    new MessageDigestResourceVersion()
            ));
            getResourceSettings().setJavaScriptCompressor(new GoogleClosureJavaScriptCompressor(CompilationLevel.SIMPLE_OPTIMIZATIONS));
        }

        getFrameworkSettings().setSerializer(new DeflatedJavaSerializer(getApplicationKey()));

        configureBootstrap();
        configureResourceBundles();

        new AnnotatedMountScanner().scanPackage("de.agilecoders.wicket.samples.pages").mount(this);
    }

    /**
     * configure all resource bundles (css and js)
     */
    private void configureResourceBundles() {
        setHeaderResponseDecorator(new RenderJavaScriptToFooterHeaderResponseDecorator());

        getResourceBundles().addJavaScriptBundle(WicketApplication.class, "core.js",
                                                 (JavaScriptResourceReference) getJavaScriptLibrarySettings().getJQueryReference(),
                                                 (JavaScriptResourceReference) getJavaScriptLibrarySettings().getWicketEventReference(),
                                                 (JavaScriptResourceReference) getJavaScriptLibrarySettings().getWicketAjaxReference(),
                                                 (JavaScriptResourceReference) ModernizrJavaScriptReference.INSTANCE
        );

        getResourceBundles().addJavaScriptBundle(WicketApplication.class, "bootstrap.js",
                                                 (JavaScriptResourceReference) Bootstrap.getSettings().getJsResourceReference(),
                                                 (JavaScriptResourceReference) Bootstrap.getSettings().getJqueryPPResourceReference(),
                                                 (JavaScriptResourceReference) BootstrapPrettifyJavaScriptReference.INSTANCE,
                                                 ApplicationJavaScript.INSTANCE
        );

        getResourceBundles().addJavaScriptBundle(WicketApplication.class, "bootstrap-extensions.js",
                                                 JQueryUIJavaScriptReference.instance(),
                                                 Html5PlayerJavaScriptReference.instance()
        );

        getResourceBundles().addCssBundle(WicketApplication.class, "bootstrap-extensions.css",
                                          Html5PlayerCssReference.instance()
        );

        getResourceBundles().addCssBundle(WicketApplication.class, "application.css",
                                          (CssResourceReference) Bootstrap.getSettings().getResponsiveCssResourceReference(),
                                          (CssResourceReference) BootstrapPrettifyCssReference.INSTANCE,
                                          FixBootstrapStylesCssResourceReference.INSTANCE
        );
    }

    private void configureBootstrap() {
        BootstrapSettings settings = new BootstrapSettings();
        settings.minify(true) // use minimized version of all bootstrap references
                .useJqueryPP(true)
                .useModernizr(true)
                .useResponsiveCss(true)
                .setJsResourceFilterName("footer-container")
                .getBootstrapLessCompilerSettings().setUseLessCompiler(false);

        ThemeProvider themeProvider = new BootswatchThemeProvider() {{
            add(new MetroTheme());
            defaultTheme("wicket");
        }};
        settings.setThemeProvider(themeProvider);

        Bootstrap.install(this, settings);
    }

    public Properties getProperties() {
        return properties;
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        try {
            properties.load(getClass().getResourceAsStream("/config.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }
}
