/*******************************************************************************
 * Copyright (c) 2014 fortiss GmbH
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Monika Wenger
 *     - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.fordiac.ide.fbtypeeditor.servicesequence.properties;

import org.eclipse.fordiac.ide.fbtypeeditor.servicesequence.editparts.OutputPrimitiveEditPart;
import org.eclipse.fordiac.ide.model.libraryElement.OutputPrimitive;
import org.eclipse.jface.viewers.IFilter;

public class OutputPrimitiveFilter implements IFilter {

	@Override
	public boolean select(final Object toTest) {
		return (toTest instanceof OutputPrimitiveEditPart) || (toTest instanceof OutputPrimitive);
	}

}
