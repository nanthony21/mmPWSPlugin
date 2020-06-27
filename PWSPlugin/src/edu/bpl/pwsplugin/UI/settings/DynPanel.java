/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.UI.settings;

import edu.bpl.pwsplugin.UI.utils.SingleBuilderJPanel;
import edu.bpl.pwsplugin.settings.DynSettings;
import edu.bpl.pwsplugin.settings.PWSPluginSettings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import edu.bpl.pwsplugin.UI.utils.ImprovedJSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author nick
 */
public class DynPanel extends SingleBuilderJPanel<DynSettings>{
    private final ImprovedJSpinner wvSpinner = new ImprovedJSpinner();
    private final ImprovedJSpinner framesSpinner = new ImprovedJSpinner();
    private final ImprovedJSpinner exposureSpinner = new ImprovedJSpinner();
    private final JComboBox<String> imConfName = new JComboBox<>();

    
    public DynPanel() {
        super(new MigLayout(), DynSettings.class);
        wvSpinner.setModel(new SpinnerNumberModel(550, 400,1000, 5));
        framesSpinner.setModel(new SpinnerNumberModel(200, 1, 1000, 1));
        exposureSpinner.setModel(new SpinnerNumberModel(50, 1, 500, 5));
        
        
        super.add(new JLabel("Wavelength (nm)"), "gapleft push");
        super.add(wvSpinner, "wrap");
        super.add(new JLabel("Exposure (ms)"), "gapleft push");
        super.add(exposureSpinner, "wrap, growx");
        super.add(new JLabel("# of Frames"), "gapleft push");
        super.add(framesSpinner, "wrap");
        super.add(new JLabel("Imaging Configuration"), "span");
        super.add(imConfName, "span");
    }
    
    @Override
    public Map<String, Object> getPropertyFieldMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("exposure", exposureSpinner);
        map.put("wavelength", wvSpinner);
        map.put("numFrames", framesSpinner);
        map.put("imConfigName", imConfName);
        return map;
    }
    
    //API
    public void setExposure(double exposureMs) {
        this.exposureSpinner.setValue(exposureMs);
    }
    
    public void setAvailableConfigNames(List<String> names) {
        this.imConfName.removeAllItems();
        if (names.isEmpty()) {
            this.imConfName.addItem("NONE!"); //Prevent a null pointer error.
        } else {
            for (String name : names) {
                this.imConfName.addItem(name);
            }
        }
    }
}
