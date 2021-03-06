/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.acquisitionSequencer.factory;

import edu.bpl.pwsplugin.UI.utils.BuilderJPanel;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerConsts;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerSettings;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.EveryNTimes;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.Step;
import edu.bpl.pwsplugin.utils.JsonableParam;
import javax.swing.JLabel;
import edu.bpl.pwsplugin.UI.utils.ImprovedComponents;
import javax.swing.SpinnerNumberModel;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author Nick Anthony <nickmanthony at hotmail.com>
 */
public class EveryNTimesFactory extends StepFactory {
    @Override
    public Class<? extends BuilderJPanel> getUI() {
        return EveryNTimesUI.class;
    }
    
    @Override
    public Class<? extends JsonableParam> getSettings() {
        return SequencerSettings.EveryNTimesSettings.class;
    }
    
    @Override
    public Class<? extends Step> getStep() {
        return EveryNTimes.class;
    }
    
    @Override
    public String getDescription() {
        return "Execute sub-steps once every `N` iterations of this this step. Offset the cycle by `offset` iterations.";
    }
    
    @Override
    public String getName() {
        return "Once per `N` iterations";
    }
    
    @Override
    public String getCategory() {
        return"Logical";
    }

    @Override
    public SequencerConsts.Type getType() {
        return SequencerConsts.Type.EVERYN;
    }
}


class EveryNTimesUI extends BuilderJPanel<SequencerSettings.EveryNTimesSettings> {
    private ImprovedComponents.Spinner n;
    private ImprovedComponents.Spinner offset;
    
    public EveryNTimesUI() {
        super(new MigLayout("insets 0 0 0 0"), SequencerSettings.EveryNTimesSettings.class);
        
        n = new ImprovedComponents.Spinner(new SpinnerNumberModel(2, 1, 1000, 1));
        offset = new ImprovedComponents.Spinner(new SpinnerNumberModel(0, 0, 1000, 1));
        
        this.add(new JLabel("N:"), "gapleft push");
        this.add(n, "wrap");
        this.add(new JLabel("Offset:"), "gapleft push");
        this.add(offset, "wrap");
    }
    
    public SequencerSettings.EveryNTimesSettings build() {
        SequencerSettings.EveryNTimesSettings settings = new SequencerSettings.EveryNTimesSettings();
        settings.n = (Integer) this.n.getValue();
        settings.offset = (Integer) this.offset.getValue();
        return settings;
    }
    
    public void populateFields(SequencerSettings.EveryNTimesSettings settings) {
        this.n.setValue(settings.n);
        this.offset.setValue(settings.offset);
    }
}
