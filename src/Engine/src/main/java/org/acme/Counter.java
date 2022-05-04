package org.acme;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;

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

    @JsonCreator
    public Counter(@JsonProperty("count") int count, @JsonProperty("hosts") String[] hosts) {
        this.Count = count;
        this.Hosts = hosts;
    }

    @Override
    public String toString() {
      return "Counter " + this.Count;
    }
}
