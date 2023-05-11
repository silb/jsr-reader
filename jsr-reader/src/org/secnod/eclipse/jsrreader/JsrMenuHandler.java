package org.secnod.eclipse.jsrreader;

import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.secnod.jsr.Jsr;
import org.secnod.jsr.JsrId;
import org.secnod.jsr.index.JsrIndex;
import org.secnod.jsr.store.JsrStore;

/**
 * For opening a JSR from the context menu.
 */
public class JsrMenuHandler extends AbstractHandler {

    public static final String COMMAND_ID = "org.secnod.eclipse.jsrreader.menu.openjsr";
    public static final String JSR_NUMBER_PARAM = "org.secnod.jsrreader.jsrNumber";
    public static final String JSR_VARIANT_PARAM = "org.secnod.jsrreader.jsrVariant";

    public JsrMenuHandler() {
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        JsrIndex jsrIndex = Activator.getDefault().index;
        JsrStore jsrStore = Activator.getDefault().jsrStore;

        Integer jsrNumber = event.getParameters().containsKey(JSR_NUMBER_PARAM) ? Integer.parseInt(event.getParameter(JSR_NUMBER_PARAM)) : null;
        String jsrVariant = event.getParameter(JSR_VARIANT_PARAM);

        Jsr jsr = null;
        if (jsrNumber != null) {
            JsrId id = JsrId.of(jsrNumber, jsrVariant);
            jsr = jsrIndex.queryById(id);
            if (jsr == null) {
                MessageDialog.openInformation(window.getShell(), "JSR not found",
                        "No data could not be found for " + id + ".");
                return null;
            }
        } else {
            jsr = findJsrFromSelection(event, window);
            if (jsr == null) {
                MessageDialog.openInformation(window.getShell(), "No relevant JSR",
                        "No relevant JSR could be found for the current selection.");
                return null;
            }
        }

        File jsrFile = jsrStore.find(jsr.id);
        if (jsrFile != null) {
            OpenJsrHandler.openJsr(window, jsr.id, jsrFile);
        } else {
            new DownloadJsrJob(jsr.id).schedule();
        }

        return null;
    }

    private Jsr findJsrFromSelection(ExecutionEvent event, IWorkbenchWindow window) throws ExecutionException {
        ISelection activeSelection = HandlerUtil.getActiveMenuSelection(event);
        String activePartId = HandlerUtil.getActivePartId(event);
        IEditorInput editorInput = HandlerUtil.getActiveEditorInput(event);
        String packageName = Activator.getDefault().selectionHandler.packageNameFromSelection(activePartId, activeSelection, editorInput);

        Jsr jsr = Activator.getDefault().index.jsrNumberForPackage(packageName);

        if (jsr == null) {
            Status status = new Status(IStatus.INFO, Activator.PLUGIN_ID, "No JSR found for package " + packageName);
            Activator.getDefault().getLog().log(status);
        } else {
            Status status = new Status(IStatus.INFO, Activator.PLUGIN_ID, jsr.id + " for package " + packageName);
            Activator.getDefault().getLog().log(status);
        }

        return jsr;
    }
}
