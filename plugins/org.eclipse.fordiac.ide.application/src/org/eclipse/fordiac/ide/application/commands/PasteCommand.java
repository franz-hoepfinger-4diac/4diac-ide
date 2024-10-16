/*******************************************************************************
 * Copyright (c) 2008 - 2017 Profactor GmbH, TU Wien ACIN, AIT, fortiss GmbH
 * 				 2019 Johannes Kepler University Linz
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Gerhard Ebenhofer, Alois Zoitl, Filip Andren, Matthias Plasch
 *   - initial API and implementation and/or initial documentation
 *   Alois Zoitl - reworked paste to also handle cut elements
 *******************************************************************************/
package org.eclipse.fordiac.ide.application.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.fordiac.ide.application.Messages;
import org.eclipse.fordiac.ide.gef.utilities.ElementSelector;
import org.eclipse.fordiac.ide.model.NameRepository;
import org.eclipse.fordiac.ide.model.commands.create.AbstractConnectionCreateCommand;
import org.eclipse.fordiac.ide.model.commands.create.AdapterConnectionCreateCommand;
import org.eclipse.fordiac.ide.model.commands.create.DataConnectionCreateCommand;
import org.eclipse.fordiac.ide.model.commands.create.EventConnectionCreateCommand;
import org.eclipse.fordiac.ide.model.libraryElement.AdapterDeclaration;
import org.eclipse.fordiac.ide.model.libraryElement.Event;
import org.eclipse.fordiac.ide.model.libraryElement.FBNetwork;
import org.eclipse.fordiac.ide.model.libraryElement.FBNetworkElement;
import org.eclipse.fordiac.ide.model.libraryElement.IInterfaceElement;
import org.eclipse.fordiac.ide.model.libraryElement.LibraryElementFactory;
import org.eclipse.fordiac.ide.model.libraryElement.Position;
import org.eclipse.fordiac.ide.model.libraryElement.StructManipulator;
import org.eclipse.fordiac.ide.model.libraryElement.VarDeclaration;
import org.eclipse.fordiac.ide.ui.errormessages.ErrorMessenger;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.swt.graphics.Point;

/**
 * The Class PasteCommand.
 */
public class PasteCommand extends Command {

	private static final int DEFAULT_DELTA = 20;
	private final Collection<? extends Object> templates;
	private final FBNetwork dstFBNetwork;
	private FBNetwork srcFBNetwork = null;

	private final Map<FBNetworkElement, FBNetworkElement> copiedElements = new HashMap<>();

	private final List<FBNetworkElement> elementsToCopy = new ArrayList<>();

	private final Set<ConnectionReference> connectionsToCopy = new HashSet<>();

	private final CompoundCommand connCreateCmds = new CompoundCommand();

	private int xDelta;
	private int yDelta;
	private boolean calcualteDelta = false;
	private Point pasteRefPos;

	private CutAndPasteFromSubAppCommand cutPasteCmd;

	/**
	 * Instantiates a new paste command.
	 *
	 * @param templates   the elements that should be copied to the destination
	 * @param destination the destination fbnetwork where the elements should be
	 *                    copied to
	 * @param pasteRefPos the reference position for pasting the elements
	 */
	public PasteCommand(final List<? extends Object> templates, final FBNetwork destination, final Point pasteRefPos) {
		this.templates = templates;
		this.dstFBNetwork = destination;
		this.pasteRefPos = pasteRefPos;
		calcualteDelta = true;

	}

	public PasteCommand(final List<? extends Object> templates, final FBNetwork destination, final int copyDeltaX,
			final int copyDeltaY) {
		this.templates = templates;
		this.dstFBNetwork = destination;
		xDelta = copyDeltaX;
		yDelta = copyDeltaY;
	}

	@Override
	public boolean canExecute() {
		return (null != templates) && (null != dstFBNetwork);
	}

	@Override
	public void execute() {
		if (dstFBNetwork != null) {
			ErrorMessenger.pauseMessages();
			gatherCopyData();
			copyFBs();
			copyConnections();
			ElementSelector.selectViewObjects(copiedElements.values());
			if (!ErrorMessenger.unpauseMessages().isEmpty()) {
				ErrorMessenger.popUpErrorMessage(
						Messages.PasteRecreateNotPossible,
						ErrorMessenger.USE_DEFAULT_TIMEOUT);
			}
		}
	}

	@Override
	public void undo() {
		connCreateCmds.undo();
		dstFBNetwork.getNetworkElements().removeAll(copiedElements.values());
		if (cutPasteCmd != null) {
			cutPasteCmd.undo();
		}
		ElementSelector.selectViewObjects(templates);

	}

	@Override
	public void redo() {
		dstFBNetwork.getNetworkElements().addAll(copiedElements.values());
		connCreateCmds.redo();
		if (cutPasteCmd != null) {
			cutPasteCmd.redo();
		}
		ElementSelector.selectViewObjects(copiedElements.values());
	}

	private void gatherCopyData() {
		int x = Integer.MAX_VALUE;
		int y = Integer.MAX_VALUE;

		for (final Object object : templates) {
			if (object instanceof FBNetworkElement) {
				final FBNetworkElement element = (FBNetworkElement) object;
				if (null == srcFBNetwork) {
					srcFBNetwork = element.getFbNetwork();
				}
				elementsToCopy.add(element);
				x = Math.min(x, element.getPosition().getX());
				y = Math.min(y, element.getPosition().getY());
			} else if (object instanceof ConnectionReference) {
				connectionsToCopy.add((ConnectionReference) object);
			} else if (object instanceof FBNetwork) {
				srcFBNetwork = (FBNetwork) object;
			}
		}

		updateDelta(x, y);
	}

	private void updateDelta(final int x, final int y) {
		if (calcualteDelta) {
			if (null != pasteRefPos) {
				xDelta = pasteRefPos.x - x;
				yDelta = pasteRefPos.y - y;
			} else {
				xDelta = DEFAULT_DELTA;
				yDelta = DEFAULT_DELTA;
			}
		}
	}

	private void copyFBs() {
		for (final FBNetworkElement element : elementsToCopy) {
			final FBNetworkElement copiedElement = createElementCopyFB(element);
			copiedElements.put(element, copiedElement);
			dstFBNetwork.getNetworkElements().add(copiedElement);
			copiedElement.setName(NameRepository.createUniqueName(copiedElement, element.getName()));
		}
	}

	private FBNetworkElement createElementCopyFB(final FBNetworkElement element) {
		final FBNetworkElement copiedElement = EcoreUtil.copy(element);
		// clear the connection references
		for (final IInterfaceElement ie : copiedElement.getInterface().getAllInterfaceElements()) {
			if (ie.isIsInput()) {
				ie.getInputConnections().clear();
			} else {
				ie.getOutputConnections().clear();
			}
		}
		copiedElement.setPosition(calculatePastePos(element));
		copiedElement.setMapping(null);

		if (copiedElement instanceof StructManipulator) {
			// structmanipulators may destroy the param values during copy
			checkDataValues(element, copiedElement);
		}

		return copiedElement;
	}

	private static void checkDataValues(final FBNetworkElement src, final FBNetworkElement copy) {
		final EList<VarDeclaration> srcList = src.getInterface().getInputVars();
		final EList<VarDeclaration> copyList = copy.getInterface().getInputVars();

		for (int i = 0; i < srcList.size(); i++) {
			final VarDeclaration srcVar = srcList.get(i);
			final VarDeclaration copyVar = copyList.get(i);
			if (null == copyVar.getValue()) {
				copyVar.setValue(LibraryElementFactory.eINSTANCE.createValue());
			}
			if (null != srcVar.getValue()) {
				copyVar.getValue().setValue(srcVar.getValue().getValue());
			}
		}
	}

	private void copyConnections() {
		for (final ConnectionReference connRef : connectionsToCopy) {
			final FBNetworkElement copiedSrc = copiedElements.get(connRef.getSourceElement());
			final FBNetworkElement copiedDest = copiedElements.get(connRef.getDestinationElement());

			if ((null != copiedSrc) || (null != copiedDest)) {
				// Only copy if one end of the connection is copied as well otherwise we will
				// get a duplicate connection
				final AbstractConnectionCreateCommand cmd = getConnectionCreateCmd(connRef.getSource());
				if (null != cmd) {
					copyConnection(connRef, copiedSrc, copiedDest, cmd);
					if (cmd.canExecute()) { // checks if the resulting connection is valid
						connCreateCmds.add(cmd);
					}
				}
			}
		}
		connCreateCmds.execute();
	}

	private AbstractConnectionCreateCommand getConnectionCreateCmd(final IInterfaceElement source) {
		AbstractConnectionCreateCommand cmd = null;
		if (source instanceof Event) {
			cmd = new EventConnectionCreateCommand(dstFBNetwork);
		} else if (source instanceof AdapterDeclaration) {
			cmd = new AdapterConnectionCreateCommand(dstFBNetwork);
		} else if (source instanceof VarDeclaration) {
			cmd = new DataConnectionCreateCommand(dstFBNetwork);
		}
		return cmd;
	}

	private void copyConnection(final ConnectionReference connRef, final FBNetworkElement copiedSrc, final FBNetworkElement copiedDest,
			final AbstractConnectionCreateCommand cmd) {
		final IInterfaceElement source = getInterfaceElement(connRef.getSource(), copiedSrc);
		final IInterfaceElement destination = getInterfaceElement(connRef.getDestination(), copiedDest);

		cmd.setSource(source);
		cmd.setDestination(destination);
		cmd.setArrangementConstraints(connRef.getRoutingData());
	}

	private IInterfaceElement getInterfaceElement(final IInterfaceElement orig, final FBNetworkElement copiedElement) {
		if (null != copiedElement) {
			// we have a copied connection target get the interface element from it
			return copiedElement.getInterfaceElement(orig.getName());
		} else if (dstFBNetwork.equals(srcFBNetwork)) {
			// we have a connection target to an existing FBNElement, only retrieve the
			// interface element if the target FBNetwrok is the same as the source. In this
			// case it is save to return the original interface element.
			return orig;
		}
		return null;
	}

	private Position calculatePastePos(final FBNetworkElement element) {
		final Position pastePos = LibraryElementFactory.eINSTANCE.createPosition();
		pastePos.setX(element.getPosition().getX() + xDelta);
		pastePos.setY(element.getPosition().getY() + yDelta);
		return pastePos;
	}

	protected CutAndPasteFromSubAppCommand getCutPasteCmd() {
		return cutPasteCmd;
	}

	public void setCutPasteCmd(final CutAndPasteFromSubAppCommand cutPasteCmd) {
		this.cutPasteCmd = cutPasteCmd;
	}

}
