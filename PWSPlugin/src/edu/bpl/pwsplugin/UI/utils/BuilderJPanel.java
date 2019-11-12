
package edu.bpl.pwsplugin.UI.utils;

import edu.bpl.pwsplugin.utils.UIBuildable;
import java.awt.LayoutManager;
import java.util.List;
import javax.swing.JPanel;

/**
 *
 * @author Nick Anthony <nickmanthony at hotmail.com>
 */
public abstract class BuilderJPanel<T> extends JPanel{
    protected Class<T> typeParamClass;

    public BuilderJPanel(LayoutManager layout, Class<T> clazz) {
        super(layout);
        this.typeParamClass = clazz;
    }
    
    public abstract T build();
    public abstract void populateFields(T t);
}