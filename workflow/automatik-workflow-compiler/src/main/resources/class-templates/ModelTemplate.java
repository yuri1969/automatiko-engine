package org.jbpm.process.codegen;

import java.util.Map;
import java.util.HashMap;

public class XXXModel implements io.automatik.engine.api.Model {
    
    private String id;
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getId() {
        return this.id;
    }
    
    @Override
    public Map<String, Object> toMap() {
        
    }
    
    @Override
    public void fromMap(Map<String, Object> params) {
        fromMap(null, params);
    }

    public void fromMap(String id, Map<String, Object> params) {
        
    }
}