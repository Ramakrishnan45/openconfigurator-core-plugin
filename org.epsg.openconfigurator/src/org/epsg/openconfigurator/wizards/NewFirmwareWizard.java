/*******************************************************************************
 * @file   NewFirmwareWizard.java
 *
 * @author Sree Hari Vignesh, Kalycito Infotech Private Limited.
 *
 * @copyright (c) 2017, Kalycito Infotech Private Limited
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

package org.epsg.openconfigurator.wizards;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.epsg.openconfigurator.console.OpenConfiguratorMessageConsole;
import org.epsg.openconfigurator.lib.wrapper.NodeAssignment;
import org.epsg.openconfigurator.lib.wrapper.OpenConfiguratorCore;
import org.epsg.openconfigurator.lib.wrapper.Result;
import org.epsg.openconfigurator.model.FirmwareManager;
import org.epsg.openconfigurator.model.IControlledNodeProperties;
import org.epsg.openconfigurator.model.Module;
import org.epsg.openconfigurator.model.Node;
import org.epsg.openconfigurator.model.PowerlinkRootNode;
import org.epsg.openconfigurator.util.OpenConfiguratorProjectUtils;
import org.epsg.openconfigurator.xmlbinding.projectfile.FirmwareList;
import org.epsg.openconfigurator.xmlbinding.projectfile.FirmwareList.Firmware;
import org.epsg.openconfigurator.xmlbinding.projectfile.TCN;
import org.jdom2.JDOMException;

/**
 * Wizard page to add new firmware.
 *
 * @author Sree Hari
 *
 */
public class NewFirmwareWizard extends Wizard {

    private static final String WINDOW_TITLE = "POWERLINK firmware wizard";

    /**
     * Add validateFirmwareWizardPage
     */
    private final ValidateFirmwareWizardPage validateFirmwarePage;

    /**
     * Selected node object. The new node will be added below this node.
     */
    private Node selectedNodeObj;

    private Module selectedModuleObj;

    private Firmware firmwareObj;

    private Object nodeOrModuleObj;

    public NewFirmwareWizard(PowerlinkRootNode nodeList, Object selectedObj) {
        if (selectedObj == null) {
            System.err.println("Invalid node selection");
        }
        nodeOrModuleObj = selectedObj;
        Object nodeModel = null;
        Object moduleModel = null;
        if (selectedObj instanceof Node) {
            Node node = (Node) selectedObj;
            selectedNodeObj = node;
            if (selectedNodeObj != null) {
                nodeModel = selectedNodeObj.getNodeModel();
            }
            if (nodeModel == null) {
                System.err.println("The NodeModel is empty!");
            }
        } else if (selectedObj instanceof Module) {
            Module module = (Module) selectedObj;
            selectedModuleObj = module;
            if (selectedModuleObj != null) {
                moduleModel = selectedModuleObj.getModelOfModule();
            }
            if (moduleModel == null) {
                System.err.println("The NodeModel is empty!");
            }
        }

        setWindowTitle(WINDOW_TITLE);
        validateFirmwarePage = new ValidateFirmwareWizardPage(selectedObj);
    }

    /**
     * Add wizard page
     */
    @Override
    public void addPages() {
        super.addPages();
        addPage(validateFirmwarePage);
    }

    private Node getNode(Object obj) {
        if (obj instanceof Node) {
            Node node = (Node) obj;
            return node;
        }
        if (obj instanceof Module) {
            Module module = (Module) obj;
            return module.getNode();
        }
        return null;
    }

    @Override
    public boolean canFinish() {
        boolean b = validateFirmwarePage.isPageComplete();
        System.out.println(b);

        if (!validateFirmwarePage.isPageComplete()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean performFinish() {
        Path firmwareFilePath = validateFirmwarePage
                .getFirmwareConfigurationPath();
        Result res = new Result();
        FirmwareList firmwareList = new FirmwareList();
        Firmware firmware = new Firmware();
        firmware.setURI(firmwareFilePath.toString());

        firmwareObj = firmware;
        FirmwareManager firmwareMngr = new FirmwareManager(nodeOrModuleObj,
                validateFirmwarePage.getFirmwareModel(), firmwareObj);

        byte[] venId = String.valueOf(firmwareMngr.getVendorId()).getBytes();
        firmware.setVendorId(venId);

        try {
            OpenConfiguratorProjectUtils.importFirmwareFile(firmwareFilePath,
                    firmwareMngr);
        } catch (IOException e) {
            System.err.println("The firmware file import is not successfull.");
            e.printStackTrace();
        }

        try {
            OpenConfiguratorProjectUtils.addFirmwareList(firmwareMngr,
                    nodeOrModuleObj, firmwareObj);
        } catch (JDOMException | IOException e) {
            System.err.println(
                    "The project file update of firmware element fails.");
            e.printStackTrace();
        }
        Node node = getNode(nodeOrModuleObj);
        res = OpenConfiguratorCore.GetInstance().AddNodeAssignment(
                node.getNetworkId(), node.getCnNodeId(),
                NodeAssignment.NMT_NODEASSIGN_SWUPDATE);

        if (!res.IsSuccessful()) {
            OpenConfiguratorMessageConsole.getInstance()
                    .printLibraryErrorMessage(res);
        }

        res = OpenConfiguratorCore.GetInstance().AddNodeAssignment(
                node.getNetworkId(), node.getCnNodeId(),
                NodeAssignment.NMT_NODEASSIGN_SWVERSIONCHECK);
        if (!res.IsSuccessful()) {
            OpenConfiguratorMessageConsole.getInstance()
                    .printLibraryErrorMessage(res);
        }

        Object nodeModelObj = node.getNodeModel();
        if (nodeModelObj != null) {
            if (nodeModelObj instanceof TCN) {
                TCN cn = (TCN) nodeModelObj;
                cn.setAutoAppSwUpdateAllowed(true);
                cn.setVerifyAppSwVersion(true);
            }
        }

        try {
            OpenConfiguratorProjectUtils.updateNodeAttributeValue(node,
                    IControlledNodeProperties.CN_VERIFY_APP_SW_VERSION_OBJECT,
                    "true");
            OpenConfiguratorProjectUtils.updateNodeAttributeValue(node,
                    IControlledNodeProperties.CN_AUTO_APP_SW_UPDATE_ALLOWED_OBJECT,
                    "true");
        } catch (JDOMException | IOException e1) {
            System.err.println(
                    "The node assignment value is not updated in the project file.");
            e1.printStackTrace();
        }

        try {
            firmwareMngr.getProject().refreshLocal(IResource.DEPTH_INFINITE,
                    new NullProgressMonitor());
        } catch (CoreException e) {
            e.printStackTrace();
        }

        return true;
    }
}
