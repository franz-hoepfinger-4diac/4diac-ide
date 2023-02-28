/*
 * generated by Xtext 2.29.0
 */
package org.eclipse.fordiac.ide.globalconstantseditor.ui;

import com.google.inject.Injector;
import org.eclipse.fordiac.ide.globalconstantseditor.ui.internal.GlobalconstantseditorActivator;
import org.eclipse.xtext.ui.guice.AbstractGuiceAwareExecutableExtensionFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * This class was generated. Customizations should only happen in a newly
 * introduced subclass. 
 */
public class GlobalConstantsExecutableExtensionFactory extends AbstractGuiceAwareExecutableExtensionFactory {

	@Override
	protected Bundle getBundle() {
		return FrameworkUtil.getBundle(GlobalconstantseditorActivator.class);
	}
	
	@Override
	protected Injector getInjector() {
		GlobalconstantseditorActivator activator = GlobalconstantseditorActivator.getInstance();
		return activator != null ? activator.getInjector(GlobalconstantseditorActivator.ORG_ECLIPSE_FORDIAC_IDE_GLOBALCONSTANTSEDITOR_GLOBALCONSTANTS) : null;
	}

}
