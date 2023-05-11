package org.secnod.eclipse.jsrreader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.secnod.jsr.JsrId;
import org.secnod.jsr.screenscraper.DownloadedFile;
import org.secnod.jsr.screenscraper.JsrDownloadScreenScraper;
import org.secnod.jsr.screenscraper.JsrZipFileInspector;
import org.secnod.jsr.screenscraper.ScreenScrapeException;
import org.secnod.jsr.screenscraper.UrlFetcher;
import org.secnod.jsr.store.JsrStore;

class DownloadJsrJob extends Job {

    private ILog log = Activator.getDefault().getLog();
    private JsrStore jsrStore = Activator.getDefault().jsrStore;
    final JsrId jsr;

    public DownloadJsrJob(JsrId jsr) {
        super("Download " + jsr);
        this.jsr = jsr;
        setUser(true);
    }

    @Override
    public boolean belongsTo(Object family) {
        return Activator.JOB_FAMILY.equals(family);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Downloading " + jsr, IProgressMonitor.UNKNOWN);
        monitor.subTask("Locating download URL");
        URI url;
        try {
            JsrDownloadScreenScraper sc = new JsrDownloadScreenScraper(jsr);
            sc.openDetailsPage();
            canceledCheck(monitor);
            do {
                sc.openReleasePage();
                canceledCheck(monitor);
                sc.openDownloadPage();
                canceledCheck(monitor);
                url = sc.findReleaseDownload();
                canceledCheck(monitor);
            } while (sc.nextRelease());
            if (url == null) return handleFailedToLocate(jsr, null);
        } catch (ScreenScrapeException e) {
            return handleFailedToLocate(jsr, e);
        } catch (IOException e) {
            return handleFailedToLocate(jsr, e);
        }

        canceledCheck(monitor);

        log.log(new Status(IStatus.INFO, Activator.PLUGIN_ID, jsr + ", URL: " + url));

        monitor.subTask("Downloading");
        File jsrFile = null;

        try {
            DownloadedFile tmp = new UrlFetcher(url).download();

            canceledCheck(monitor);

            if (tmp.file.getName().endsWith(".pdf")) {
                jsrFile = jsrStore.add(jsr, tmp.file);
            } else if (tmp.file.getName().endsWith(".zip")) {
                JsrZipFileInspector zipFileInspector = new JsrZipFileInspector(tmp.file);
                File pdfFromZip = zipFileInspector.copySpec();

                canceledCheck(monitor);

                if (pdfFromZip != null) {
                    Status zipFileStatus = new Status(IStatus.INFO, Activator.PLUGIN_ID, "Found PDF " + pdfFromZip.getName() + " in ZIP file: " + tmp);
                    log.log(zipFileStatus);
                    canceledCheck(monitor);
                    jsrFile = jsrStore.add(jsr, pdfFromZip);
                } else {
                    Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not find PDF file in ZIP file " + tmp + " downloaded for " + jsr);
                    log.log(status);
                    return status;
                }
            } else {
                Status unknownFileStatus = new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Cannot open file for " + jsr + ": " + tmp);
                log.log(unknownFileStatus);
                return unknownFileStatus;
            }
        } catch (IOException e) {
            return handleFailedToDownload(e, jsr, url);
        } catch (URISyntaxException e) {
            return handleFailedToDownload(e, jsr, url);
        }

        canceledCheck(monitor);

        if (jsrFile == null) {
            Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Unknown error, unable to open " + jsr + ".");
            log.log(status);
            return status;
        }

        canceledCheck(monitor);

        final File finalJsrFile = jsrFile;
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                try {
                    OpenJsrHandler.openJsr(window, jsr, finalJsrFile);
                } catch (ExecutionException e) {
                    throw new RuntimeException("Failed to open file " + finalJsrFile + " for " + jsr, e);
                }
            }
        });

        monitor.done();
        return Status.OK_STATUS;
    }

    private IStatus handleFailedToDownload(Throwable cause, JsrId jsr, URI url) {
        String message = "Failed to download " + jsr + " from URL: " + url;
        Status status = new Status(IStatus.INFO, Activator.PLUGIN_ID, message);
        log.log(status);
        return status;
    }

    private IStatus handleFailedToLocate(JsrId jsr, Throwable cause) {
        IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Unable to find download URL for " + jsr + ".");
        log.log(status);
        return status;
    }

    private void canceledCheck(IProgressMonitor m) {
        if (m.isCanceled()) {
            throw new OperationCanceledException();
        }
    }

}