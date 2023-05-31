package org.secnod.eclipse.jsrreader;

import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.secnod.jsr.JsrId;

/**
 * For downloading / opening any JSR specified in a prompt.
 * The JSR may not exist in the index.
 */
public class OpenJsrHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

        IInputValidator validator = new IInputValidator() {
            @Override
            public String isValid(String newText) {
                if (newText == null || newText.isEmpty())
                    return "";
                return JsrId.of(newText) != null
                        ? null
                        : "Must be a number and an optional space and word";
            }
        };
        InputDialog prompt = new InputDialog(window.getShell(), "Open a JSR",
                "Specify a JSR with an optional variant", null, validator);
        //            prompt.setBlockOnOpen(true);
        if (InputDialog.OK != prompt.open())
            return null;
        JsrId id = JsrId.of(prompt.getValue());

        File jsrFile = Activator.getDefault().jsrStore.find(id);

        if (jsrFile != null)
            openJsr(window, id, jsrFile);
        else
            new DownloadJsrJob(id).schedule();

        return null;
    }

    static void openJsr(IWorkbenchWindow window, JsrId jsrId, File jsrFile) throws ExecutionException {
        try {
            IFileStore fileStore = EFS.getStore(jsrFile.toURI());
            IDE.openEditorOnFileStore(window.getActivePage(), fileStore);
        } catch (PartInitException e) {
            throw new ExecutionException("", e);
        } catch (CoreException e) {
            throw new ExecutionException("", e);
        }

        Status status = new Status(IStatus.INFO, Activator.PLUGIN_ID, jsrId + ", file: " + jsrFile);
        Activator.getDefault().getLog().log(status);
    }
}
