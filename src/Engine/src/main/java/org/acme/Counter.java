package org.acme;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = false)
public class Counter {
    @JsonProperty("count")
    public int Count;
    
    @JsonProperty("hosts")
    public String[] Hosts;

    public Counter() {
        super();
        this.Count = 0;
        this.Hosts = new String[0];        
    }

    public Counter(int count, String[] hosts) {
        this.Count = count;
        this.Hosts = hosts;
    }
}
