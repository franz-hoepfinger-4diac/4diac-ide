/*
 * generated by Xtext 2.29.0
 */
package org.eclipse.fordiac.ide.globalconstantseditor.ui.tests;

import com.google.inject.Injector;
import org.eclipse.fordiac.ide.globalconstantseditor.ui.internal.GlobalconstantseditorActivator;
import org.eclipse.xtext.testing.IInjectorProvider;

public class GlobalConstantsUiInjectorProvider implements IInjectorProvider {

	@Override
	public Injector getInjector() {
		return GlobalconstantseditorActivator.getInstance().getInjector("org.eclipse.fordiac.ide.globalconstantseditor.GlobalConstants");
	}

}
