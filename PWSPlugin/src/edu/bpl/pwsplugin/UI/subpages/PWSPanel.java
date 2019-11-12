/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.UI.subpages;

import edu.bpl.pwsplugin.UI.utils.SingleBuilderJPanel;
import edu.bpl.pwsplugin.settings.Settings;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import net.miginfocom.swing.MigLayout;
import org.micromanager.PropertyMap;

/**
 *
 * @author nick
 */
public class PWSPanel extends SingleBuilderJPanel<Settings.PWSSettings>{
    private JSpinner exposureSpinner;
    private JSpinner wvStartSpinner;
    private JSpinner wvStopSpinner;
    private JSpinner wvStepSpinner;
    private JCheckBox ttlTriggerCheckbox = new JCheckBox("Use TTL Sequencing");
    private JCheckBox externalTriggerCheckBox = new JCheckBox("Use External TTL Trigger");
    
    public PWSPanel() {
        super(new MigLayout(), Settings.PWSSettings.class);
        
        exposureSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 1000, 5));
        wvStartSpinner = new JSpinner(new SpinnerNumberModel(500, 400, 1000, 5));
        wvStartSpinner.setToolTipText("In nanometers. The wavelength to start scanning at.");

        wvStopSpinner = new JSpinner(new SpinnerNumberModel(700, 400, 1000, 5));
        wvStopSpinner.setToolTipText("In nanometers. The wavelength to stop scanning at.");

        wvStepSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 50, 1));
        
        ttlTriggerCheckbox.setToolTipText("Whether the camera should be configured to trigger wavelength changes in the filter over TTL. This may not be supported.");
        ttlTriggerCheckbox.addActionListener((evt)->{
            boolean checked = ttlTriggerCheckbox.isSelected();
            if (!checked) {
                externalTriggerCheckBox.setSelected(false);
            }
            externalTriggerCheckBox.setEnabled(checked);
        });
                
        
        externalTriggerCheckBox.setToolTipText("Whether the filter should trigger a new camera acquisition over TTL. This is not possible for LCTF but can be done with the VF-5 Filter.");
        externalTriggerCheckBox.setEnabled(false);
                
        super.add(new JLabel("Start (nm"));
        super.add(new JLabel("Stop (nm)"));
        super.add(new JLabel("Step (nm)"));
        super.add(new JLabel("Exposure (ms)"), "wrap");
        super.add(wvStartSpinner);
        super.add(wvStopSpinner);
        super.add(wvStepSpinner);
        super.add(exposureSpinner, "wrap");
        super.add(ttlTriggerCheckbox, "wrap");
        super.add(externalTriggerCheckBox, "wrap");
    }

    @Override
    public Map<String, JComponent> getPropertyFieldMap() {
        Map<String, JComponent> map = new HashMap<String, JComponent>();
        map.put("wvStart", wvStartSpinner);
        map.put("wvStop", wvStopSpinner);
        map.put("wvStep", wvStepSpinner);
        map.put("exposure", exposureSpinner);
        map.put("ttlTriggering", ttlTriggerCheckbox);
        map.put("externalCamTriggering", externalTriggerCheckBox);
        return map;
    }
}