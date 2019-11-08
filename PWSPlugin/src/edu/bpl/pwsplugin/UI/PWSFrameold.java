///////////////////////////////////////////////////////////////////////////////
//PROJECT:       PWS Plugin for Micro-Manager
//
//-----------------------------------------------------------------------------
//
// AUTHOR:      Nick Anthony 2019
//
// COPYRIGHT:    Northwestern University, Evanston, IL 2019
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
//
package edu.bpl.pwsplugin.UI;


import edu.bpl.pwsplugin.AcqManager;
import edu.bpl.pwsplugin.Globals;
import edu.bpl.pwsplugin.PWSPlugin;
import java.awt.Color;
import java.io.File;
import org.micromanager.internal.utils.MMFrame;
import org.micromanager.propertymap.MutablePropertyMapView;
import org.micromanager.LogManager;
import java.util.ArrayList;
import javax.swing.DefaultComboBoxModel;
import mmcorej.StrVector;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.JButton;
import org.micromanager.internal.utils.FileDialogs;
import org.micromanager.internal.utils.ReportingUtils;
import javax.swing.SwingWorker;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.ArrayUtils;

public class PWSFrame extends MMFrame {

    private MutablePropertyMapView settings_;
    private final LogManager log_;
    private AcqManager acqManager_;
    private boolean otherSettingsStale_ = true;
    private boolean PWSSettingsStale_ = true;
    private boolean saveSettingsStale_ = true;
    private boolean DYNSettingsStale_ = true;
    private boolean FLSettingsStale_ = true;
    /**
     * 
     */
    public PWSFrame(AcqManager manager) {
        acqManager_ = manager;
        settings_ = Globals.mm().profile().getSettings(PWSFrame.class);
        log_ = Globals.mm().logs();
        
        super.setTitle(String.format("%s %s", PWSPlugin.menuName, PWSPlugin.versionNumber));
        this.initComponents();
        this.addDocListeners();
        this.scanDevices();
        this.scanFilterBlock();
        
        try { //load settings
            wvStartField.setText(String.valueOf(settings_.getInteger(PWSPlugin.Settings.start, 500)));
            wvStopField.setText(String.valueOf(settings_.getInteger(PWSPlugin.Settings.stop,700)));
            wvStepField.setText(String.valueOf(settings_.getInteger(PWSPlugin.Settings.step,2)));
            directoryText.setText(settings_.getString(PWSPlugin.Settings.savePath,""));
            hardwareSequencingCheckBox.setSelected(settings_.getBoolean(PWSPlugin.Settings.sequence,false));
            externalTriggerCheckBox.setSelected(settings_.getBoolean(PWSPlugin.Settings.externalTrigger,false));
            cellNumEdit.setText(String.valueOf(settings_.getInteger(PWSPlugin.Settings.cellNum, 1)));
            systemNameEdit.setText(settings_.getString(PWSPlugin.Settings.systemName, ""));
            darkCountsEdit.setText(String.valueOf(settings_.getInteger(PWSPlugin.Settings.darkCounts, 0)));
            double[] linArray = settings_.getDoubleList(PWSPlugin.Settings.linearityPoly);
            if (linArray.length > 0) {
                linearityCorrectionEdit.setText(StringUtils.join(ArrayUtils.toObject(linArray), ","));
            } else {
                linearityCorrectionEdit.setText("null");
            }
            exposureEdit.setText(String.valueOf(settings_.getDouble(PWSPlugin.Settings.exposure, 100.0)));
            dynExposureEdit.setText(String.valueOf(settings_.getDouble(PWSPlugin.Settings.dynExposure, 50.0)));
            dynFramesEdit.setText(String.valueOf(settings_.getInteger(PWSPlugin.Settings.dynNumFrames, 200)));
            dynWvEdit.setText(String.valueOf(settings_.getInteger(PWSPlugin.Settings.dynWavelength, 550)));
            flExposureEdit.setText(String.valueOf(settings_.getDouble(PWSPlugin.Settings.flExposure, 1000)));
            flWvEdit.setText(String.valueOf(settings_.getInteger(PWSPlugin.Settings.flWavelength, 550)));
            altCamCheckBox.setSelected(settings_.getBoolean(PWSPlugin.Settings.altCamFl, false));
            double[] camTransformArray = settings_.getDoubleList(PWSPlugin.Settings.camTransform);
            if (camTransformArray.length > 0) {
                altCamTransformEdit.setText(StringUtils.join(ArrayUtils.toObject(camTransformArray), ","));
            } else {
                altCamTransformEdit.setText("null");
            }
            //Do this last in case the filter is not available and an error is thrown.
            filterComboBox.setSelectedItem(settings_.getString(PWSPlugin.Settings.filterLabel, ""));
            flFilterBlockCombo.setSelectedItem(settings_.getString(PWSPlugin.Settings.flFilterBlock, ""));
            altCamNameCombo.setSelectedItem(settings_.getString(PWSPlugin.Settings.flAltCamName, ""));
        }
        catch (Exception e) {
            ReportingUtils.logError(e);
        }
        super.loadAndRestorePosition(200, 200);
    }       
    
    private void saveSettings() {
        try{
            int start = Integer.parseInt(wvStartField.getText().trim());
            int stop = Integer.parseInt(wvStopField.getText().trim());
            int step = Integer.parseInt(wvStepField.getText().trim());
            int darkCounts = Integer.parseInt(darkCountsEdit.getText().trim());
            String linText = linearityCorrectionEdit.getText().trim();
            double[] linearityPolynomial;
            if ((linText.equals("None")) || (linText.equals("null"))) {
                linearityPolynomial = null;
            } else {
                linearityPolynomial = Arrays.asList(linText.split(","))
                                .stream()
                                .map(String::trim)
                                .mapToDouble(Double::parseDouble).toArray();
            }
            ArrayList<Integer> wvList = new ArrayList<Integer>();
            for (int i = start; i <= stop; i += step) {
                wvList.add(i);
            }   
            int[] wvArr = new int[wvList.size()];
            for (int i=0; i<wvList.size(); i++) {
                wvArr[i] = wvList.get(i).intValue();
            }
            settings_.putIntegerList(PWSPlugin.Settings.wv, wvArr);
            settings_.putInteger(PWSPlugin.Settings.start, start);
            settings_.putInteger(PWSPlugin.Settings.stop, stop);
            settings_.putInteger(PWSPlugin.Settings.step, step);    
            settings_.putInteger(PWSPlugin.Settings.darkCounts, darkCounts);
            settings_.putDoubleList(PWSPlugin.Settings.linearityPoly, linearityPolynomial);
            settings_.putString(PWSPlugin.Settings.systemName, systemNameEdit.getText());
            settings_.putBoolean(PWSPlugin.Settings.sequence, hardwareSequencingCheckBox.isSelected());
            settings_.putBoolean(PWSPlugin.Settings.externalTrigger,externalTriggerCheckBox.isSelected());
            settings_.putString(PWSPlugin.Settings.savePath, directoryText.getText());
            settings_.putInteger(PWSPlugin.Settings.cellNum, Integer.parseInt(cellNumEdit.getText()));
            settings_.putDouble(PWSPlugin.Settings.exposure, Double.parseDouble(exposureEdit.getText()));
            settings_.putDouble(PWSPlugin.Settings.dynExposure, Double.parseDouble(dynExposureEdit.getText()));
            settings_.putInteger(PWSPlugin.Settings.dynNumFrames, Integer.parseInt(dynFramesEdit.getText()));
            settings_.putInteger(PWSPlugin.Settings.dynWavelength, Integer.parseInt(dynWvEdit.getText()));
            settings_.putDouble(PWSPlugin.Settings.flExposure, Double.parseDouble(flExposureEdit.getText()));
            settings_.putInteger(PWSPlugin.Settings.flWavelength, Integer.parseInt(flWvEdit.getText()));
            settings_.putBoolean(PWSPlugin.Settings.altCamFl, altCamCheckBox.isSelected());
            String transformText = altCamTransformEdit.getText().trim();
            double[] transform;
            if (transformText.equals("null")) {
                transform = null;
            } else {
                transform = Arrays.asList(transformText.split(",")).stream().map(String::trim).mapToDouble(Double::parseDouble).toArray();
            }
            if (transform.length==6 || transform==null) {
                settings_.putDoubleList(PWSPlugin.Settings.camTransform, transform);
            } else {
                ReportingUtils.showError("The camera transform must be of length 6. Translates to a 2x3 affine transformation.");
            }
        }
        catch(NumberFormatException e){
            log_.showMessage("A valid number was not specified.");
        }
        try{
            settings_.putString(PWSPlugin.Settings.filterLabel, filterComboBox.getSelectedItem().toString());
            settings_.putString(PWSPlugin.Settings.flFilterBlock, flFilterBlockCombo.getSelectedItem().toString());
            settings_.putString(PWSPlugin.Settings.flAltCamName, altCamNameCombo.getSelectedItem().toString());
        }
        catch(NumberFormatException e){
            log_.showMessage("A valid string was not specified.");
        }
        catch (Exception e){
            log_.logError(e);
        }
    }
    
    @Override
    public void dispose() {
        saveSettings();
        super.dispose();
    }
    
    private void scanDevices() {
        String[] devs = Globals.core().getLoadedDevices().toArray();
        //Search for tunable spectral filters.
        StrVector newDevs = new StrVector();
        for (int i = 0; i < devs.length; i++) {
            try {
                if (Globals.core().hasProperty(devs[i], "Wavelength")) {
                    newDevs.add(devs[i]);
                }
            }
            catch (Exception ex) {}
        }
        DefaultComboBoxModel model = new DefaultComboBoxModel(newDevs.toArray());
        filterComboBox.setModel(model); //Update the available names.
        String oldName = settings_.getString("filtLabel","");
            if (Arrays.asList(newDevs.toArray()).contains(oldName)) {
                filterComboBox.setSelectedItem(oldName);
            }
    }
    
    private void scanFilterBlock() {
        Iterator<String> filterSettings = Globals.core().getAvailableConfigs("Filter").iterator();
        StrVector settings = new StrVector();
        while (filterSettings.hasNext()) {
            settings.add(filterSettings.next());
        }
        if (settings.size() == 0) {
            acqManager_.automaticFlFilterEnabled = false;
            ReportingUtils.showMessage("Micromanager is missing a `Filter` config group which is needed for automated fluorescence. The first setting of the group should be the filter block used for PWS");
        } else {
            acqManager_.automaticFlFilterEnabled = true;
            DefaultComboBoxModel model = new DefaultComboBoxModel(settings.toArray());
            flFilterBlockCombo.setModel(model); //Update the available names.
        }
        
        Iterator<String> cameras = Globals.core().getAvailableConfigs("Camera").iterator();
        StrVector camSettings = new StrVector();
        while (cameras.hasNext()) {
            camSettings.add(cameras.next());
        }
        if (camSettings.size()==0) {
            altCamCheckBox.setSelected(false);
            altCamCheckBox.setEnabled(false);
            ReportingUtils.showMessage("Could not find a `Camera` config group. This group should be set to allow switching between multiple cameras for different imaging modalities.");
        } else {
            DefaultComboBoxModel model = new DefaultComboBoxModel(camSettings.toArray());
            altCamNameCombo.setModel(model);
        }
    }
    
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel11 = new javax.swing.JPanel();
        flWvEdit = new javax.swing.JTextField();
        stepLabel6 = new javax.swing.JLabel();
        stepLabel7 = new javax.swing.JLabel();
        flExposureEdit = new javax.swing.JTextField();
        flFilterBlockCombo = new javax.swing.JComboBox<>();
        stepLabel8 = new javax.swing.JLabel();
        altCamCheckBox = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        altCamTransformEdit = new javax.swing.JTextField();
        altCamNameCombo = new javax.swing.JComboBox<>();
        jPanel9 = new javax.swing.JPanel();
        cellNumEdit = new javax.swing.JTextField();
        stepLabel1 = new javax.swing.JLabel();
        directoryText = new javax.swing.JTextField();
        directoryButton = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        filterLabel = new javax.swing.JLabel();
        filterComboBox = new javax.swing.JComboBox<String>();
        jPanel1 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        darkCountsEdit = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        systemNameEdit = new javax.swing.JTextField();
        jPanel6 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        linearityCorrectionEdit = new javax.swing.JTextField();
        acqPWSButton = new javax.swing.JButton();
        acqDynButton = new javax.swing.JButton();
        acqFlButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        flWvEdit.setText("550");

        stepLabel6.setText("Wavelength (nm)");

        stepLabel7.setText("Filter Block");

        flExposureEdit.setText("100");

        flFilterBlockCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        flFilterBlockCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                flFilterBlockComboActionPerformed(evt);
            }
        });

        stepLabel8.setText("Exposure (ms)");

        altCamCheckBox.setText("Use Alternate Camera");
        altCamCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                altCamCheckBoxActionPerformed(evt);
            }
        });

        jLabel4.setText("Camera Name");

        jLabel5.setText("Affine Transform");

        altCamTransformEdit.setText("jTextField2");

        altCamNameCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        altCamNameCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                altCamNameComboActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel11Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel11Layout.createSequentialGroup()
                                .addComponent(altCamCheckBox)
                                .addGap(16, 16, 16)
                                .addComponent(stepLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel4)
                                .addComponent(flExposureEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(altCamNameCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(stepLabel6)
                    .addComponent(flWvEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(flFilterBlockCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stepLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(altCamTransformEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(58, Short.MAX_VALUE))
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(stepLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stepLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(altCamCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(flExposureEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(flFilterBlockCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(stepLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(flWvEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(altCamTransformEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(altCamNameCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(38, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Fluorescence", jPanel11);

        cellNumEdit.setText("1");
        cellNumEdit.setToolTipText("Cell Number");

        stepLabel1.setText("Cell Number");

        directoryButton.setText("...");
        directoryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                directoryButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel9Layout.createSequentialGroup()
                        .addComponent(directoryText, javax.swing.GroupLayout.PREFERRED_SIZE, 337, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(directoryButton))
                    .addGroup(jPanel9Layout.createSequentialGroup()
                        .addComponent(stepLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cellNumEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(directoryButton)
                    .addComponent(directoryText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cellNumEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stepLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(76, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Save Path", jPanel9);

        jPanel5.setLayout(new java.awt.GridLayout(2, 2));

        filterLabel.setText("Filter");
        jPanel5.add(filterLabel);

        filterComboBox.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        filterComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterComboBoxActionPerformed(evt);
            }
        });
        jPanel5.add(filterComboBox);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(406, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(124, 124, 124))
        );

        jTabbedPane1.addTab("Hardware", jPanel4);

        jPanel8.setLayout(new java.awt.GridLayout(2, 1));

        jPanel7.setLayout(new java.awt.GridLayout(2, 2));

        jLabel2.setText("Dark Counts");
        jLabel2.setToolTipText("# of counts per pixel when the camera is not exposed to any light. E.g if measuring dark counts with 2x2 binning the number here should be 1/4 of your measurement 2x2 binning pools 4 pixels.");
        jPanel7.add(jLabel2);

        darkCountsEdit.setToolTipText("# of counts per pixel when the camera is not exposed to any light. E.g if measuring dark counts with 2x2 binning the number here should be 1/4 of your measurement 2x2 binning pools 4 pixels.");
        jPanel7.add(darkCountsEdit);

        jLabel1.setText("Name");
        jLabel1.setToolTipText("The name of the system.");
        jPanel7.add(jLabel1);

        systemNameEdit.setToolTipText("The name of the system.");
        jPanel7.add(systemNameEdit);

        jPanel8.add(jPanel7);

        jPanel6.setLayout(new java.awt.GridLayout(2, 1));

        jLabel3.setText("Linearity Correction");
        jLabel3.setToolTipText("Comma separated values representing the polynomial to linearize the counts from the camera. In the form \"A,B,C\" = Ax + Bx^2 + Cx^3. Type \"None\" or \"null\" if correction is not needed.");
        jPanel6.add(jLabel3);

        linearityCorrectionEdit.setToolTipText("Comma separated values representing the polynomial to linearize the counts from the camera. In the form \"A,B,C\" = Ax + Bx^2 + Cx^3. Type \"None\" or \"null\" if correction is not needed.");
        jPanel6.add(linearityCorrectionEdit);

        jPanel8.add(jPanel6);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(293, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("System Data", jPanel1);

        acqPWSButton.setText("Acquire PWS");
        acqPWSButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                acqPWSButtonActionPerformed(evt);
            }
        });

        acqDynButton.setText("Acquire Dynamics");
        acqDynButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                acqDynButtonActionPerformed(evt);
            }
        });

        acqFlButton.setText("Acquire Fluorescence");
        acqFlButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                acqFlButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(acqPWSButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(acqDynButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(acqFlButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(acqPWSButton)
                    .addComponent(acqDynButton)
                    .addComponent(acqFlButton)))
        );

        jTabbedPane1.getAccessibleContext().setAccessibleName("General");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
    }//GEN-LAST:event_formWindowClosing

    private void acqPWSButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_acqPWSButtonActionPerformed
        acquirePWS();
    }//GEN-LAST:event_acqPWSButtonActionPerformed

    private void acqDynButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_acqDynButtonActionPerformed
        acquireDynamics();
    }//GEN-LAST:event_acqDynButtonActionPerformed

    private void acqFlButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_acqFlButtonActionPerformed
        acquireFluorescence();
    }//GEN-LAST:event_acqFlButtonActionPerformed

    private void filterComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterComboBoxActionPerformed
        PWSSettingsChanged();
    }//GEN-LAST:event_filterComboBoxActionPerformed

    private void directoryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_directoryButtonActionPerformed
        File f = FileDialogs.openDir(this, "Directory to save to",
            new FileDialogs.FileType("SaveDir", "Save Directory", "D:\\Data", true, ""));
        directoryText.setText(f.getAbsolutePath());
    }//GEN-LAST:event_directoryButtonActionPerformed

    private void altCamNameComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_altCamNameComboActionPerformed
        FLSettingsChanged();
    }//GEN-LAST:event_altCamNameComboActionPerformed

    private void altCamCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_altCamCheckBoxActionPerformed
        FLSettingsChanged();
    }//GEN-LAST:event_altCamCheckBoxActionPerformed

    private void flFilterBlockComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_flFilterBlockComboActionPerformed
        FLSettingsChanged();
    }//GEN-LAST:event_flFilterBlockComboActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton acqDynButton;
    private javax.swing.JButton acqFlButton;
    private javax.swing.JButton acqPWSButton;
    private javax.swing.JCheckBox altCamCheckBox;
    private javax.swing.JComboBox<String> altCamNameCombo;
    private javax.swing.JTextField altCamTransformEdit;
    private javax.swing.JTextField cellNumEdit;
    private javax.swing.JTextField darkCountsEdit;
    private javax.swing.JButton directoryButton;
    private javax.swing.JTextField directoryText;
    private javax.swing.JComboBox<String> filterComboBox;
    private javax.swing.JLabel filterLabel;
    private javax.swing.JTextField flExposureEdit;
    private javax.swing.JComboBox<String> flFilterBlockCombo;
    private javax.swing.JTextField flWvEdit;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField linearityCorrectionEdit;
    private javax.swing.JLabel stepLabel1;
    private javax.swing.JLabel stepLabel6;
    private javax.swing.JLabel stepLabel7;
    private javax.swing.JLabel stepLabel8;
    private javax.swing.JTextField systemNameEdit;
    // End of variables declaration//GEN-END:variables

    private SwingWorker<Void, Void> runInBackground(JButton button, Runnable myFunc) {
        //This funciton will run myFunc in a separate thread. `button` will be disabled while the function is running.
        return new SwingWorker<Void, Void>() {
            Object o = new Object() {{button.setEnabled(false); execute();}}; //Fake constructor.
            
            @Override
            public Void doInBackground() {myFunc.run(); return null;}

            @Override
            public void done() {button.setEnabled(true);}
        };
    }
        
    private void acquire(JButton button, Runnable f) {
        try {
            configureManager();
        } catch (Exception e) {
            log_.showError(e);
            return;
        }
        SwingWorker worker = runInBackground(button, f);
    }
    
    public void acquirePWS() {
        acquire(acqPWSButton, acqManager_::acquirePWS);
    }
    
    public void acquireDynamics() {
        acquire(acqDynButton, acqManager_::acquireDynamics);
    }
    
    public void acquireFluorescence() {
        acquire(acqFlButton, acqManager_::acquireFluorescence);
    }
    
    private void configureManager() throws Exception {
        if (otherSettingsStale_ || PWSSettingsStale_ || saveSettingsStale_ || DYNSettingsStale_ || FLSettingsStale_){
            saveSettings(); 
            if (saveSettingsStale_) {
                int cellNum = settings_.getInteger(PWSPlugin.Settings.cellNum,1);
                String savePath = settings_.getString(PWSPlugin.Settings.savePath, "");
                acqManager_.setCellNum(cellNum);
                acqManager_.setSavePath(savePath);
                saveSettingsStale_ = false;
            }
            if (otherSettingsStale_) {      
                int darkCounts = settings_.getInteger(PWSPlugin.Settings.darkCounts,0);
                double[] linearityPolynomial = settings_.getDoubleList(PWSPlugin.Settings.linearityPoly);
                String systemName = settings_.getString(PWSPlugin.Settings.systemName, "");
                acqManager_.setSystemSettings(darkCounts, linearityPolynomial, systemName);
                otherSettingsStale_ = false;
            }
            if (PWSSettingsStale_) {
                int[] wv = settings_.getIntegerList(PWSPlugin.Settings.wv);
                String filtLabel = settings_.getString(PWSPlugin.Settings.filterLabel, "");
                boolean hardwareSequence = settings_.getBoolean(PWSPlugin.Settings.sequence, false);
                boolean useExternalTrigger = settings_.getBoolean(PWSPlugin.Settings.externalTrigger, false);
                double exposure = settings_.getDouble(PWSPlugin.Settings.exposure, 100);
                acqManager_.setPWSSettings(exposure, useExternalTrigger, hardwareSequence, wv, filtLabel);
                PWSSettingsStale_ = false;
            }        
            if (DYNSettingsStale_) {
                double exposure = settings_.getDouble(PWSPlugin.Settings.dynExposure, 100);
                String filterLabel = settings_.getString(PWSPlugin.Settings.filterLabel, "");
                int wavelength = settings_.getInteger(PWSPlugin.Settings.dynWavelength, 550);
                int numFrames = settings_.getInteger(PWSPlugin.Settings.dynNumFrames, 200);
                acqManager_.setDynamicsSettings(exposure, filterLabel, wavelength, numFrames);
                DYNSettingsStale_ = false;
            }
            if (FLSettingsStale_) {
                double exposure = settings_.getDouble(PWSPlugin.Settings.flExposure, 1000);
                String flFilterBlock = settings_.getString(PWSPlugin.Settings.flFilterBlock, "");
                if (settings_.getBoolean(PWSPlugin.Settings.altCamFl, false)) {
                    String flCamera = settings_.getString(PWSPlugin.Settings.flAltCamName, "");
                    double[] camTransformPlaceholder = {1.0, 2.0 , 3.0, 4.0, 5.0, 6.0};
                    double[] camTransform = settings_.getDoubleList(PWSPlugin.Settings.camTransform, camTransformPlaceholder);
                    if (camTransform.length != 6){
                        ReportingUtils.showError("The affine transformation for the alternate fluorescence camera is not of length 6!");
                    }
                    acqManager_.setFluorescenceSettings(exposure, flFilterBlock, flCamera, camTransform);
                } else {
                    int wavelength = settings_.getInteger(PWSPlugin.Settings.flWavelength, 550);
                    String filterLabel = settings_.getString(PWSPlugin.Settings.filterLabel, "");
                    acqManager_.setFluoresecenceSettings(exposure, flFilterBlock, wavelength, filterLabel);
                }
            }
            acqPWSButton.setBackground(Color.green);
        }
    }
    
    
    //API
    public void setSavePath(String savepath) {
        directoryText.setText(savepath);
    }
    
    public void setCellNumber(int cellNum) {
        cellNumEdit.setText(String.valueOf(cellNum));
    }
    
    public String getFilterName() {
        return filterComboBox.getSelectedItem().toString();
    }
    
    public void setPWSExposure(double exposureMs) {
        exposureEdit.setText(String.valueOf(exposureMs));
    }
    
    public void setDynamicsExposure(double exposureMs) {
        dynExposureEdit.setText(String.valueOf(exposureMs));
    }
    
    public void setFluorescenceExposure(double exposureMs) {
        flExposureEdit.setText(String.valueOf(exposureMs));
    }
    
    public void setFluorescenceFilter(String filterBlockName) {
       if (!this.getFluorescenceFilterNames().contains(filterBlockName)) {
           ReportingUtils.showMessage(filterBlockName + " is not a valid filter block name.");
       } else {
        flFilterBlockCombo.setSelectedItem(filterBlockName);
       }
    }
    
    public Vector<String> getFluorescenceFilterNames() {
        Vector<String> names = new Vector<String>();
        for (int i=0; i<flFilterBlockCombo.getItemCount(); i++) {
            names.add(flFilterBlockCombo.getItemAt(i));
        }
        return names;
    }
    
    public void setFluorescenceEmissionWavelength(int wv) {
        flWvEdit.setText(String.valueOf(wv));
    }
}
