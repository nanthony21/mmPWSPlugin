/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.acquisitionSequencer.factory;

import edu.bpl.pwsplugin.acquisitionSequencer.steps.Step;
import edu.bpl.pwsplugin.UI.utils.BuilderJPanel;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerConsts;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerSettings;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.ZStackStep;
import edu.bpl.pwsplugin.utils.JsonableParam;
import javax.swing.JButton;
import javax.swing.JLabel;
import edu.bpl.pwsplugin.UI.utils.ImprovedComponents;
import javax.swing.SpinnerNumberModel;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author nick
 */
public class ZStackFactory extends StepFactory {
    @Override
    public Class<? extends BuilderJPanel> getUI() {
        return ZStackUI.class;
    }
    
    @Override
    public Class<? extends JsonableParam> getSettings() {
        return SequencerSettings.ZStackSettings.class;
    }
    
    @Override
    public Class<? extends Step> getStep() {
        return ZStackStep.class;
    }
    
    @Override
    public String getDescription() {
        return "Repeat the enclosed steps at evenly spaced positions along the Z axis.";
    }
    
    @Override 
    public String getName() {
        return "Z-Stack";
    }
    
    @Override
    public String getCategory() {
        return "Sequencing";
    }
    
    @Override 
    public SequencerConsts.Type getType() {
        return SequencerConsts.Type.ZSTACK;
    }
}
    

class ZStackUI extends BuilderJPanel<SequencerSettings.ZStackSettings> {
    private final ImprovedComponents.Spinner intervalUm = new ImprovedComponents.Spinner(new SpinnerNumberModel(1.0, -100.0, 100.0, 0.5));
    private final ImprovedComponents.Spinner numStacks = new ImprovedComponents.Spinner(new SpinnerNumberModel(2, 1, 1000, 1));
    private final ImprovedComponents.Spinner startingPosition = new ImprovedComponents.Spinner(new SpinnerNumberModel(0.0, -10000.0, 10000.0, 1.0));
    //private final JComboBox<String> deviceName = new JComboBox<>();
    private static final String ABSOLUTE = "Absolute Position";
    private static final String RELATIVE = "Relative Position";
    private final JButton absolute = new JButton(ABSOLUTE);
    
    public ZStackUI() {
        super(new MigLayout(), SequencerSettings.ZStackSettings.class);
        
        
        //Layout
        //add(new JLabel("Stage:"), "gapleft push");
        //add(deviceName, "growx, wrap");
        add(new JLabel("# of positions:"), "gapleft push");
        add(numStacks, "wrap, growx");
        add(new JLabel("Z interval (um):"), "gapleft push");
        add(intervalUm, "wrap, growx");
        add(absolute, "growx, wrap, spanx");
        add(new JLabel("Starting position (um):"), "gapleft push");
        add(startingPosition, "wrap, growx");
 
        
        //Functionality
        absolute.addActionListener((evt) -> {
            if (absolute.getText().equals(ABSOLUTE)) {
                setAbsolute(false);
            } else {
                setAbsolute(true);
            }
        });
        
        //updateComboBox();
    }
    
    private void setAbsolute(boolean absolute) {
        startingPosition.setEnabled(absolute);
        if (absolute) { this.absolute.setText(ABSOLUTE); }
        else { this.absolute.setText(RELATIVE); }
    }
    
    /*private void updateComboBox() {
        String[] devNames = Globals.core().getLoadedDevicesOfType(DeviceType.StageDevice).toArray();
        //deviceName.setModel(new DefaultComboBoxModel<>(devNames));
    }*/
    
    @Override
    public SequencerSettings.ZStackSettings build() {
        SequencerSettings.ZStackSettings settings = new SequencerSettings.ZStackSettings();
        settings.numStacks = (Integer) numStacks.getValue();
        settings.intervalUm = (Double) intervalUm.getValue();
        settings.startingPosition = (Double) startingPosition.getValue(); // This is only used if absolute is true
        settings.absolute = absolute.getText().equals(ABSOLUTE);
        //settings.deviceName = (String) deviceName.getSelectedItem();
        return settings;
    }
    
    @Override
    public void populateFields(SequencerSettings.ZStackSettings settings) {
        numStacks.setValue(settings.numStacks);
        intervalUm.setValue(settings.intervalUm);
        startingPosition.setValue(settings.startingPosition);
        setAbsolute(settings.absolute);
        //deviceName.setSelectedItem(settings.deviceName);
    }
}