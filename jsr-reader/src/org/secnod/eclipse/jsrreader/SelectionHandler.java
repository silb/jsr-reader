package org.secnod.eclipse.jsrreader;

import static java.lang.String.format;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;

public class SelectionHandler {

    // TODO Extract interface ISelectionHandler, implementation base classes:
    // EditorSelectionHandler and TypedSelectionHandler

    private static final String JAVA_SOURCE_EDITOR_ID = "org.eclipse.jdt.ui.CompilationUnitEditor";
    private static final String JAVA_CLASS_EDITOR_ID = "org.eclipse.jdt.ui.ClassFileEditor";

    private static String packageNameFromJavaEditor(ITextSelection textSelection, IEditorInput editorInput)
            throws JavaModelException {
        ITypeRoot root = (ITypeRoot) JavaUI.getEditorInputJavaElement(editorInput);
        IJavaElement[] javaElements = root.codeSelect(textSelection.getOffset(), textSelection.getLength());
        if (javaElements.length == 0) return null;
        IJavaElement javaElement = javaElements[0];
        //IJavaElement javaElement = root.getElementAt(textSelection.getOffset());
        return PackageName.of(javaElement);
    }

    private static String packageNameFromStructuredSelection(IStructuredSelection selection) throws JavaModelException {
        Object first = selection.getFirstElement();
//        Status status = new Status(IStatus.INFO, Activator.PLUGIN_ID, "first: " + first.getClass().getName());
//        Activator.getDefault().getLog().log(status);
        return first instanceof IJavaElement ? PackageName.of((IJavaElement) first) : null;
    }

    /*
     * Potential extension points:
     *   - for other types of structured selections
     *   - for other the underlying model of other types of editors
     */
    public String packageNameFromSelection(String activePartId, ISelection activeSelection, IEditorInput editorInput) {
        try {
            if (activeSelection == null) return null;
            String from = null;
            String packageName = null;
            if (editorInput != null
                    && (JAVA_SOURCE_EDITOR_ID.equals(activePartId) || JAVA_CLASS_EDITOR_ID.equals(activePartId))) {
                from = "Java editor selection";
                packageName = packageNameFromJavaEditor((ITextSelection) activeSelection, editorInput);
            } else if (activeSelection instanceof IStructuredSelection) {
                from = IStructuredSelection.class.getSimpleName();
                packageName = packageNameFromStructuredSelection((IStructuredSelection) activeSelection);
            } else if (activeSelection instanceof ITextSelection) {
                // For any other type of editor, assume the selected text is a package name
                ITextSelection textSelection = (ITextSelection) activeSelection;
                from = ITextSelection.class.getSimpleName();
                packageName = textSelection.getText();
            }

            String message = packageName != null
                    ? format("Package name from %s: %s", from, packageName)
                    : format("No package name from %s: %s ", from, activeSelection.getClass().getSimpleName());
            Status status = new Status(IStatus.INFO, Activator.PLUGIN_ID, message);
            Activator.getDefault().getLog().log(status);
            return packageName;
        } catch (JavaModelException e) {
            Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failure while obtaining package name", e);
            Activator.getDefault().getLog().log(status);
            return null;
        }
    }
}
