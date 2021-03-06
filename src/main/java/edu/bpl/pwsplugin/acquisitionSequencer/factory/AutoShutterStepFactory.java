/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.acquisitionSequencer.factory;

import edu.bpl.pwsplugin.Globals;
import edu.bpl.pwsplugin.UI.utils.BuilderJPanel;
import edu.bpl.pwsplugin.UI.utils.DirectorySelector;
import edu.bpl.pwsplugin.UI.utils.ImprovedComponents;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerConsts;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerSettings;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.AutoShutterStep;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.RootStep;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.Step;
import edu.bpl.pwsplugin.hardware.configurations.HWConfiguration;
import edu.bpl.pwsplugin.hardware.settings.ImagingConfigurationSettings;
import edu.bpl.pwsplugin.utils.JsonableParam;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author nicke
 */
public class AutoShutterStepFactory extends StepFactory {
    //Should only exist once as the root of each experiment, sets the needed root parameters.
    @Override
    public Class<? extends BuilderJPanel> getUI() {
        return AutoShutterUI.class;
    }
    
    @Override
    public Class<? extends JsonableParam> getSettings() {
        return SequencerSettings.AutoShutterSettings.class;
    }
    
    @Override
    public Class<? extends Step> getStep() {
        return AutoShutterStep.class;
    }
    
    @Override
    public String getDescription() {
        return "Automatically enable and then disable an illuminator";
    }
    
    @Override
    public String getName() {
        return "Shutter";
    }
    
    @Override
    public String getCategory() {
        return "Utility";
    }

    @Override
    public SequencerConsts.Type getType() {
        return SequencerConsts.Type.AUTOSHUTTER;
    }
}

class AutoShutterUI extends BuilderJPanel<SequencerSettings.AutoShutterSettings> implements PropertyChangeListener {
    JComboBox<String> configName = new JComboBox<>();
    ImprovedComponents.Spinner warmupTime = new ImprovedComponents.Spinner(new SpinnerNumberModel(10.0, 0.0, 120.0, 1));
    
    public AutoShutterUI() {
        super(new MigLayout("insets 0 0 0 0"), SequencerSettings.AutoShutterSettings.class);
        
        
        this.add(new JLabel("Config Name:"));
        this.add(configName, "wrap");
        this.add(new JLabel("Warmup time (min.):"));
        this.add(warmupTime);
        
        Globals.addPropertyChangeListener(this); //Listen for config changes.
    }
    
    @Override
    public void populateFields(SequencerSettings.AutoShutterSettings settings) {
        configName.setSelectedItem(settings.configName);
        warmupTime.setValue(settings.warmupTimeMinutes);
    }
    
    @Override
    public SequencerSettings.AutoShutterSettings build() {
        SequencerSettings.AutoShutterSettings settings = new SequencerSettings.AutoShutterSettings();
        settings.configName = (String) configName.getSelectedItem();
        settings.warmupTimeMinutes = (Double) warmupTime.getValue();
        return settings;
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        //We subscribe to the Globals property changes. This gets fired when a change is detected.
        if (evt.getPropertyName().equals("config")) { //The hardware configuration changed.
            HWConfiguration cfg = (HWConfiguration) evt.getNewValue();
            setConfigNames(cfg.getSettings().configs);
        }
    }
    
    private void setConfigNames(List<ImagingConfigurationSettings> settings) {
        String[] names = new String[settings.size()];
        for (int i=0; i<settings.size(); i++) {
            names[i] = settings.get(i).name;
        }
        configName.setModel(new DefaultComboBoxModel<>(names));
    }   
}
