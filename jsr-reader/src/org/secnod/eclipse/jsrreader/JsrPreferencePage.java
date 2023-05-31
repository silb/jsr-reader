package org.secnod.eclipse.jsrreader;

import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.secnod.jsr.store.JsrStore;

public class JsrPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    public JsrPreferencePage() {}

    @Override
    public void init(IWorkbench workbench) {}

    @Override
    protected Control createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new RowLayout(SWT.VERTICAL));

        Label locationLabel = new Label(container, SWT.HORIZONTAL);
        locationLabel.setText("PDF cache location:");
        Text location = new Text(container, SWT.READ_ONLY | SWT.MULTI);
        location.setText(Activator.getDefault().jsrStore.getDirectory().toString());

        Button button = new Button(container, SWT.PUSH);
        button.setText("Wipe PDF cache");
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                JsrStore jsrStore = Activator.getDefault().jsrStore;
                try {
                    jsrStore.purge(); // XXX IO operation. Maybe run it in another thread by creating a job.
                } catch (IOException ex) {
                    IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not purge JSR PDF cache in folder " + jsrStore.getDirectory(), ex);
                    Activator.getDefault().getLog().log(status);
                    Shell shell = e.display.getActiveShell();
                    MessageDialog.openError(shell, "JSR PDF cache purge failed", status.getMessage());
                }
            }
        });
        return container;
    }
}
