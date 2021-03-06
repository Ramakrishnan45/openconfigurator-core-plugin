/*******************************************************************************
 * @file   ObjectAdapterFactory.java
 *
 * @author Ramakrishnan Periyakaruppan, Kalycito Infotech Private Limited.
 *
 * @copyright (c) 2015, Kalycito Infotech Private Limited
 *                    All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *   * Neither the name of the copyright holders nor the
 *     names of its contributors may be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package org.epsg.openconfigurator.adapters;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertySource;
import org.epsg.openconfigurator.model.Parameter;
import org.epsg.openconfigurator.model.ParameterGroup;
import org.epsg.openconfigurator.model.ParameterReference;
import org.epsg.openconfigurator.model.PowerlinkObject;
import org.epsg.openconfigurator.model.PowerlinkSubobject;

/**
 * Factory implementation of property sources for POWERLNK Objects available for
 * any node.
 *
 * @author Ramakrishnan P
 *
 */
public class ObjectAdapterFactory implements IAdapterFactory {

    private static final int MINIMUM_ACTION_BAR_SIZE_OF_PROPERTIES_PAGE = 4;

    private static final int MAXIMUM_ACTION_BAR_SIZE_OF_PROPERTIES_PAGE = 5;

    /**
     * Property source for objects.
     */
    private ObjectPropertySource objectPropertySource;

    /**
     * Property source for sub-objects.
     */
    private SubObjectPropertySource subObjectPropertySource;

    /**
     * Property source for Parameter reference.
     */
    private ParameterRefPropertySource parameterRefPropertySource;

    /**
     * Property source for Parameter.
     */
    private ParameterPropertySource parameterPropertySource;
    /**
     * Property source for Parameter group.
     */
    private ParameterGroupPropertySource parameterGroupPropertySource;

    @SuppressWarnings("unchecked")
    @Override
    public Object getAdapter(final Object adaptableObject,
            @SuppressWarnings("rawtypes") Class adapterType) {
        if (adapterType == IPropertySource.class) {
            if (adaptableObject instanceof PowerlinkObject) {
                PowerlinkObject plkObject = (PowerlinkObject) adaptableObject;

                if (objectPropertySource == null) {
                    objectPropertySource = new ObjectPropertySource(
                            (PowerlinkObject) adaptableObject);
                } else {
                    objectPropertySource
                            .setObjectData((PowerlinkObject) adaptableObject);
                }

                try {
                    IActionBars toolMenu = PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getActivePage()
                            .showView(IPageLayout.ID_PROP_SHEET).getViewSite()
                            .getActionBars();

                    toolMenu.updateActionBars();

                    IContributionItem items[] = PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getActivePage()
                            .showView(IPageLayout.ID_PROP_SHEET).getViewSite()
                            .getActionBars().getToolBarManager().getItems();
                    for (IContributionItem item : items) {

                        if (item.getId() == null) {
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                    .getActivePage()
                                    .showView(IPageLayout.ID_PROP_SHEET)
                                    .getViewSite().getActionBars()
                                    .getToolBarManager().remove(item);

                        }
                    }

                    plkObject.getNode().createActions(plkObject,
                            objectPropertySource);
                    plkObject.getNode().enableActions();

                    String value = (String) objectPropertySource
                            .getPropertyValue(
                                    AbstractObjectPropertySource.OBJ_ACTUAL_VALUE_EDITABLE_ID);
                    if (!value.isEmpty()) {
                        if (value.contains("0x")) {
                            plkObject.getNode().getDecToHexAction()
                                    .setEnabled(false);
                            toolMenu.updateActionBars();
                        } else {
                            plkObject.getNode().getHexToDecAction()
                                    .setEnabled(false);
                            toolMenu.updateActionBars();
                        }
                    } else {
                        plkObject.getNode().disableActions();
                        toolMenu.updateActionBars();
                    }

                    int length = PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getActivePage()
                            .showView(IPageLayout.ID_PROP_SHEET).getViewSite()
                            .getActionBars().getToolBarManager()
                            .getItems().length;

                    if (length <= MINIMUM_ACTION_BAR_SIZE_OF_PROPERTIES_PAGE) {

                        PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                .getActivePage()
                                .showView(IPageLayout.ID_PROP_SHEET)
                                .getViewSite().getActionBars()
                                .getToolBarManager().add(plkObject.getNode()
                                        .getConvertDecimalAction());
                    }
                    if (length <= MAXIMUM_ACTION_BAR_SIZE_OF_PROPERTIES_PAGE) {

                        PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                .getActivePage()
                                .showView(IPageLayout.ID_PROP_SHEET)
                                .getViewSite().getActionBars()
                                .getToolBarManager().add(plkObject.getNode()
                                        .getConvertHexaDecimalAction());

                    }

                    toolMenu.updateActionBars();

                } catch (PartInitException e) {
                    e.printStackTrace();
                }

                return objectPropertySource;
            } else if (adaptableObject instanceof PowerlinkSubobject) {
                PowerlinkSubobject plksubObj = (PowerlinkSubobject) adaptableObject;
                if (subObjectPropertySource == null) {
                    subObjectPropertySource = new SubObjectPropertySource(
                            (PowerlinkSubobject) adaptableObject);
                } else {
                    subObjectPropertySource.setSubObjectData(
                            (PowerlinkSubobject) adaptableObject);
                }

                try {
                    IActionBars toolMenu = PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getActivePage()
                            .showView(IPageLayout.ID_PROP_SHEET).getViewSite()
                            .getActionBars();

                    toolMenu.updateActionBars();

                    IContributionItem items[] = PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getActivePage()
                            .showView(IPageLayout.ID_PROP_SHEET).getViewSite()
                            .getActionBars().getToolBarManager().getItems();
                    for (IContributionItem item : items) {

                        if (item.getId() == null) {
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                    .getActivePage()
                                    .showView(IPageLayout.ID_PROP_SHEET)
                                    .getViewSite().getActionBars()
                                    .getToolBarManager().remove(item);

                        }
                    }

                    plksubObj.getNode().createActions(adaptableObject,
                            subObjectPropertySource);
                    plksubObj.getNode().enableActions();

                    String value = (String) subObjectPropertySource
                            .getPropertyValue(
                                    AbstractObjectPropertySource.OBJ_ACTUAL_VALUE_EDITABLE_ID);
                    if (!value.isEmpty()) {
                        if (value.contains("0x")) {
                            plksubObj.getNode().getDecToHexAction()
                                    .setEnabled(false);
                            toolMenu.updateActionBars();
                        } else {
                            plksubObj.getNode().getHexToDecAction()
                                    .setEnabled(false);
                            toolMenu.updateActionBars();
                        }
                    } else {
                        plksubObj.getNode().disableActions();
                        toolMenu.updateActionBars();
                    }

                    int length = PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getActivePage()
                            .showView(IPageLayout.ID_PROP_SHEET).getViewSite()
                            .getActionBars().getToolBarManager()
                            .getItems().length;

                    if (length <= MINIMUM_ACTION_BAR_SIZE_OF_PROPERTIES_PAGE) {

                        PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                .getActivePage()
                                .showView(IPageLayout.ID_PROP_SHEET)
                                .getViewSite().getActionBars()
                                .getToolBarManager().add(plksubObj.getNode()
                                        .getConvertDecimalAction());
                    }
                    if (length <= MAXIMUM_ACTION_BAR_SIZE_OF_PROPERTIES_PAGE) {

                        PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                .getActivePage()
                                .showView(IPageLayout.ID_PROP_SHEET)
                                .getViewSite().getActionBars()
                                .getToolBarManager().add(plksubObj.getNode()
                                        .getConvertHexaDecimalAction());

                    }
                    toolMenu.updateActionBars();

                } catch (PartInitException e) {
                    e.printStackTrace();
                }
                return subObjectPropertySource;
            } else if (adaptableObject instanceof ParameterReference) {
                ParameterReference paramRef = (ParameterReference) adaptableObject;
                paramRef.getNode().disableActions();
                if (parameterRefPropertySource == null) {
                    parameterRefPropertySource = new ParameterRefPropertySource(
                            (ParameterReference) adaptableObject);
                } else {
                    parameterRefPropertySource
                            .setModelData((ParameterReference) adaptableObject);
                }
                return parameterRefPropertySource;
            } else if (adaptableObject instanceof Parameter) {
                if (parameterPropertySource == null) {
                    parameterPropertySource = new ParameterPropertySource(
                            (Parameter) adaptableObject);
                } else {
                    parameterPropertySource
                            .setModelData((Parameter) adaptableObject);
                }
                return parameterPropertySource;
            } else if (adaptableObject instanceof ParameterGroup) {
                ParameterGroup paramGrp = (ParameterGroup) adaptableObject;
                paramGrp.getObjectDictionary().getNode().disableActions();
                if (parameterGroupPropertySource == null) {
                    parameterGroupPropertySource = new ParameterGroupPropertySource(
                            (ParameterGroup) adaptableObject);
                } else {
                    parameterGroupPropertySource
                            .setModelData((ParameterGroup) adaptableObject);
                }
                return parameterGroupPropertySource;
            } else {
                System.err.println("Not supported");
            }
        }
        return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class[] getAdapterList() {
        return new Class[] { IPropertySource.class };
    }
}
