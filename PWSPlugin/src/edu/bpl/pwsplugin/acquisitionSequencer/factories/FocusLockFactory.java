/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.acquisitionSequencer.factories;

import edu.bpl.pwsplugin.Globals;
import edu.bpl.pwsplugin.UI.utils.BuilderJPanel;
import edu.bpl.pwsplugin.UI.utils.SingleBuilderJPanel;
import edu.bpl.pwsplugin.acquisitionSequencer.AcquisitionStatus;
import edu.bpl.pwsplugin.acquisitionSequencer.Consts;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.ContainerStep;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.SequencerFunction;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.SequencerSettings;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.Step;
import edu.bpl.pwsplugin.utils.JsonableParam;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author Nick Anthony <nickmanthony at hotmail.com>
 */
public class FocusLockFactory extends StepFactory {
    @Override
    public Class<? extends BuilderJPanel> getUI() {
        return FocusLockUI.class;
    }
    
    @Override
    public Class<? extends JsonableParam> getSettings() {
        return SequencerSettings.FocusLockSettings.class;
    }
    
    @Override
    public Class<? extends Step> getStep() {
        return FocusLock.class;
    }
    
    @Override
    public String getDescription() {
        return "Engage continuous hardware autofocus";
    }
    
    @Override
    public String getName() {
        return "Optical Focus Lock";
    }
    
    @Override
    public Consts.Category getCategory() {
        return Consts.Category.UTIL;
    }

    @Override
    public Consts.Type getType() {
        return Consts.Type.PFS;
    }
}

class FocusLockUI extends SingleBuilderJPanel<SequencerSettings.FocusLockSettings>{
    JSpinner offset;
    JSpinner delay;
    private Map<String,Object> m = new HashMap<>();
    
    public FocusLockUI() {
        super(new MigLayout(), SequencerSettings.FocusLockSettings.class);
        
        offset = new JSpinner(new SpinnerNumberModel(0, -1e8, 1e8, 1));
        delay = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 30.0, 1.0));
        
        this.add(new JLabel("Z Offset"));
        this.add(offset, "wrap");
        this.add(new JLabel("Delay (s)"));
        this.add(delay);
        
        m.put("zOffset", offset);
        m.put("preDelay", delay);
    }
    
    @Override
    public Map<String, Object> getPropertyFieldMap() {        
        return m;
    }
}

class FocusLock extends ContainerStep {
    public FocusLock() {
        super(Consts.Type.PFS);
    }
    
    @Override
    public SequencerFunction getFunction() {
        SequencerFunction stepFunction = super.getSubstepsFunction();
        SequencerSettings.FocusLockSettings settings = (SequencerSettings.FocusLockSettings) this.getSettings();
        return new SequencerFunction() {
            @Override
            public AcquisitionStatus applyThrows(AcquisitionStatus status) throws Exception {
                //FocusLock A function that turns on the PFS, runs substep and then turns it off.
                Globals.core().setAutoFocusOffset(settings.zOffset);
                Globals.core().fullFocus();
                Globals.core().enableContinuousFocus(true);
                Thread.sleep((long)(settings.preDelay * 1000.0));
                AcquisitionStatus newstatus = stepFunction.apply(status);
                if (!Globals.core().isContinuousFocusLocked()) {
                    Globals.mm().logs().logMessage("Autofocus failed!");
                    status.update("Autofocus failed!", status.currentCellNum);
                }
                Globals.core().enableContinuousFocus(false);
                return newstatus;
            } 
        };
    }
}