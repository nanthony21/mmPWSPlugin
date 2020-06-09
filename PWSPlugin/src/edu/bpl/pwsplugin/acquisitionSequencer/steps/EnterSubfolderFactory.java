/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.acquisitionSequencer.steps;

import edu.bpl.pwsplugin.UI.utils.BuilderJPanel;
import edu.bpl.pwsplugin.acquisitionSequencer.AcquisitionStatus;
import edu.bpl.pwsplugin.acquisitionSequencer.Consts;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerFunction;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerSettings;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.ContainerStep;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.Step;
import edu.bpl.pwsplugin.utils.JsonableParam;
import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.JLabel;
import javax.swing.JTextField;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author Nick Anthony <nickmanthony at hotmail.com>
 */
public class EnterSubfolderFactory extends StepFactory {
     @Override
    public Class<? extends BuilderJPanel> getUI() {
        return EnterSubfolderUI.class;
    }
    
    @Override
    public Class<? extends JsonableParam> getSettings() {
        return SequencerSettings.EnterSubfolderSettings.class;
    }
    
    @Override
    public Class<? extends Step> getStep() {
        return EnterSubfolderStep.class;
    }
    
    @Override
    public String getDescription() {
        return "Change the folder that enclosed acquisitions are saved to.";
    }
    
    @Override
    public String getName() {
        return "Enter Subfolder";
    }
    
    @Override
    public Consts.Category getCategory() {
        return Consts.Category.UTIL;
    }

    @Override
    public Consts.Type getType() {
        return Consts.Type.SUBFOLDER;
    }
}

class EnterSubfolderStep extends ContainerStep {
    
    public EnterSubfolderStep() {
        super(new SequencerSettings.EnterSubfolderSettings(), Consts.Type.SUBFOLDER);
    }
    
    @Override
    public SequencerFunction getStepFunction() {
        SequencerFunction stepFunction = super.getSubstepsFunction();
        SequencerSettings.EnterSubfolderSettings settings = (SequencerSettings.EnterSubfolderSettings) this.getSettings();
        return new SequencerFunction() {
            @Override
            public AcquisitionStatus applyThrows(AcquisitionStatus status) throws Exception {
                String origPath = status.getSavePath();
                status.newStatusMessage(String.format("Moving to subfolder: %s", settings.relativePath));
                status.setSavePath(Paths.get(origPath).resolve(settings.relativePath).toString());
                status = stepFunction.apply(status);
                status.setSavePath(origPath);
                return status;
            } 
        };
    }
    
    @Override
    public Double numberNewAcqs() { return this.numberNewAcqsOneIteration(); }
    
    @Override
    public List<String> requiredRelativePaths(Integer startingCellNum) {
        List<String> l = new ArrayList<>();
        String path = ((SequencerSettings.EnterSubfolderSettings) this.getSettings()).relativePath;
        Integer cellNum = startingCellNum;
        for (Step step : this.getSubSteps()) {
            List<String> subPaths = step.requiredRelativePaths(cellNum);
            Function<String, String> f = (subPath) -> {return Paths.get(path).resolve(subPath).toString(); };
            l.addAll(subPaths.stream().map(f).collect(Collectors.toList()));
            cellNum += (int) Math.round(step.numberNewAcqs());
        }
        return l;
    }
    
    @Override
    public List<String> validate() {
        List<String> errs = super.validate();
        String path = ((SequencerSettings.EnterSubfolderSettings) this.settings).relativePath;
        try {
            Paths.get(path);
        } catch (InvalidPathException e) {
            errs.add(String.format("Relative path %s is invalid", path));
        }
        return errs;
    }
}


class EnterSubfolderUI extends BuilderJPanel<SequencerSettings.EnterSubfolderSettings> {
    private JTextField relPath = new JTextField();
    
    public EnterSubfolderUI() {
        super(new MigLayout("insets 0 0 0 0"), SequencerSettings.EnterSubfolderSettings.class);
        
        
        this.add(new JLabel("Subfolder:"));
        this.add(relPath, "wrap, pushx, growx");
    }
    
    public SequencerSettings.EnterSubfolderSettings build() {
        String p = this.relPath.getText();
        File f = new File(p);
        SequencerSettings.EnterSubfolderSettings settings = new SequencerSettings.EnterSubfolderSettings();
        settings.relativePath = p;
        return settings;
    }
    
    @Override
    public void populateFields(SequencerSettings.EnterSubfolderSettings settings) {
        this.relPath.setText(settings.relativePath.toString());
    }
}
