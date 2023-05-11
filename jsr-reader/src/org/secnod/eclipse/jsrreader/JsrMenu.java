package org.secnod.eclipse.jsrreader;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.secnod.jsr.Jsr;
import org.secnod.jsr.index.JsrIndex;

public class JsrMenu extends CompoundContributionItem {

    @Override
    protected IContributionItem[] getContributionItems() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();
        String activePartId = page.getActivePartReference() != null ? page.getActivePartReference().getId() : null;
        ISelection selection = page.getSelection();
        IEditorInput editorInput = page.getActiveEditor() != null ? page.getActiveEditor().getEditorInput() : null;

        Activator activator = Activator.getDefault();
        String packageName = activator.selectionHandler.packageNameFromSelection(activePartId, selection, editorInput);
        if (packageName == null) return new IContributionItem[0];
        JsrIndex index = activator.index;

        Jsr mainJsr = index.queryByPackage(packageName);
        if (mainJsr == null) return new IContributionItem[0];

        Collection<Jsr> jsrsForCurrentSelection = index.queryAllByPackage(packageName);
        jsrsForCurrentSelection.remove(mainJsr);

        MenuManager menuManager = new MenuManager("JSR Archive");
        for (Jsr jsr : jsrsForCurrentSelection) {
            menuManager.add(menuEntry(jsr, window, null));
        }

        ImageDescriptor icon = Activator.getImageDescriptor("icons/favicon.ico");
        return new IContributionItem[] { menuEntry(mainJsr, window, icon), menuManager };
    }

    IContributionItem menuEntry(Jsr jsr, IWorkbenchWindow window, ImageDescriptor icon) {
        CommandContributionItemParameter c = new CommandContributionItemParameter(
                window,
                null,
                JsrMenuHandler.COMMAND_ID,
                CommandContributionItem.STYLE_PUSH
                );

        StringBuilder label = new StringBuilder("Open ").append(jsr);
        if (jsr.tags != null && !jsr.tags.isEmpty()) {
            label.append(" [");
            for (Iterator<String> i = jsr.tags.iterator(); i.hasNext();) {
                label.append(i.next());
                if (i.hasNext()) {
                    label.append(", ");
                }
            }
            label.append(']');
        }

        c.label = label.toString();
        c.tooltip = jsr.title;
        c.icon = icon;

        Map<String, String> params = new HashMap<>();
        params.put(JsrMenuHandler.JSR_NUMBER_PARAM, jsr.getJsrNumber().toString());
        params.put(JsrMenuHandler.JSR_VARIANT_PARAM, jsr.getVariant());
        c.parameters = params;
        return new CommandContributionItem(c);
    }
}
