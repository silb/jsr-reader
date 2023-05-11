package org.secnod.eclipse.jsrreader;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

class PackageName {

    static String of(IJavaElement element) throws JavaModelException {
        switch (element.getElementType()) {
        case IJavaElement.FIELD:
            return fromField((IField) element);
        case IJavaElement.LOCAL_VARIABLE:
            return fromVariable((ILocalVariable) element);
        case IJavaElement.TYPE:
            return fromType((IType) element);
        case IJavaElement.PACKAGE_DECLARATION:
            return fromPackage((IPackageDeclaration) element);
        case IJavaElement.PACKAGE_FRAGMENT:
            return fromPackage((IPackageFragment) element);
        case IJavaElement.CLASS_FILE:
            return fromClassFile((IClassFile) element);
        case IJavaElement.PACKAGE_FRAGMENT_ROOT:
            return fromJarFile((IPackageFragmentRoot) element);
        }
        return null;
    }

    private static String fromJarFile(IPackageFragmentRoot jar) throws JavaModelException {
        for (IJavaElement child : jar.getChildren()) {
            IPackageFragment p = (IPackageFragment) child;
            if (p.hasSubpackages() || !p.getElementName().matches("^javax?\\..*")) continue;
            return fromPackage(p);
        }
        return null;
    }

    private static String fromClassFile(IClassFile classFile) {
        return fromPackage(classFile.findPrimaryType().getPackageFragment());
    }

    private static String fromPackage(IPackageDeclaration packageDeclaration) {
        return packageDeclaration.getElementName();
    }

    private static String fromPackage(IPackageFragment packageFragment) {
        return packageFragment.getElementName();
    }

    private static String fromType(IType type) {
        return fromPackage(type.getPackageFragment());
    }

    private static String fromVariable(ILocalVariable variable) throws JavaModelException {
        return fromSignature(variable.getTypeSignature(), variable.getDeclaringMember().getDeclaringType());
    }

    private static String fromField(IField field) throws JavaModelException {
        return fromSignature(field.getTypeSignature(), field.getDeclaringType());
    }

    private static String fromSignature(String signature, IType declaringType) throws JavaModelException {
        String simpleName = Signature.getSignatureSimpleName(signature);
        String[][] typeNames = declaringType.resolveType(simpleName);
        return typeNames != null ? typeNames[0][0] : null;
    }
}
