/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.metadata;

import edu.bpl.pwsplugin.Globals;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import mmcorej.org.json.JSONArray;
import mmcorej.org.json.JSONException;
import mmcorej.org.json.JSONObject;
import org.micromanager.internal.utils.ReportingUtils;

/**
 *
 * @author Nick Anthony <nickmanthony at hotmail.com>
 */
public class MetadataBase {
    List<Double> linearityPoly;
    String system;
    Integer darkCounts;
    String time;
                    
    public MetadataBase(List<Double> linearityPoly, String systemName, Integer darkCounts) {
        this.linearityPoly = linearityPoly;
        this.system = systemName;
        this.darkCounts = darkCounts;
        this.time =  LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));

        if (this.system.equals("")) {
            ReportingUtils.showMessage("The `system` metadata field is blank. It should contain the name of the system.");
        }
        if (this.darkCounts.equals(0)) {
            ReportingUtils.showMessage("The `darkCounts` field of the metadata is 0. This can't be right.");
        }
        
    }
    
    public MetadataBase(MetadataBase base) {
        this.linearityPoly = base.linearityPoly;
        this.system = base.system;
        this.darkCounts = base.darkCounts;
        this.time = base.time;
    }
            
    public JSONObject toJson() {
        try {
            JSONObject md = new JSONObject();
            if (this.linearityPoly.size() > 0) {
                JSONArray linPoly = new JSONArray();
                for (int i=0; i<this.linearityPoly.size(); i++) {
                    linPoly.put(this.linearityPoly.get(i));
                }
                md.put("linearityPoly", linPoly);
            } else{
                md.put("linearityPoly", JSONObject.NULL);
            }
            md.put("system", this.system);
            md.put("darkCounts", this.darkCounts);
            md.put("time", this.time);
            //TODO validate jsonschema
            return md;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
