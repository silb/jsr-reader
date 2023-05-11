package org.secnod.eclipse.jsrreader;

import java.io.File;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.secnod.jsr.index.JsrIndex;
import org.secnod.jsr.store.JsrDataStore;
import org.secnod.jsr.store.JsrMetadataStore;
import org.secnod.jsr.store.JsrStore;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.secnod.eclipse.jsrreader";

    static final String JOB_FAMILY = PLUGIN_ID;

    JsrIndex index;
    JsrStore jsrStore;  // TODO make into a service
    SelectionHandler selectionHandler = new SelectionHandler();

    private static Activator plugin;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        File stateDir = getStateLocation().toFile();
        File cacheDir = new File(stateDir, "jsr-cache");
        jsrStore = new JsrStore(cacheDir);
        // TODO make into a service / context, see
        //   http://wiki.eclipse.org/Platform_Command_Framework#Contexts
        //   http://www.linuxtopia.org/online_books/eclipse_documentation/eclipse_platform_plug-in_developer_guide/topic/org.eclipse.platform.doc.isv/guide/eclipse_platform_plugin_wrkAdv_services.htm
        //   http://www.eclipsezone.com/articles/extensions-vs-services/
        //   http://help.eclipse.org/indigo/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Freference%2Fapi%2Forg%2Feclipse%2Fui%2Fservices%2Fpackage-summary.html

        File dataFile = new File(stateDir, JsrDataStore.FILENAME);
        File metadataFile = new File(stateDir, JsrMetadataStore.FILENAME);

        index = new JsrIndex.Builder()
                .data(dataFile.exists() ? JsrDataStore.loadJson(dataFile) : JsrDataStore.loadJson())
                .metadata(metadataFile.exists() ? JsrMetadataStore.loadJson(metadataFile) : JsrMetadataStore.loadJson())
                .build();
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        IJobManager jobMan = Job.getJobManager();
        jobMan.cancel(JOB_FAMILY);
        jobMan.join(JOB_FAMILY, null);
        plugin = null;
        super.stop(context);
    }

    public static Activator getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given
     * plug-in relative path
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return ImageDescriptor.createFromURL(FileLocator.find(getDefault().getBundle(), new Path(path), null));
        //return imageDescriptorFromPlugin(PLUGIN_ID, path); // Depends on the state of the bundle
    }
}
