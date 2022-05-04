package org.acme;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = false)
public class Counter {
    public int Count;
    
    public String[] Hosts;

    public Counter() {
        this.Count = 0;
        this.Hosts = new String[0];
    }

    public Counter(int count, String[] hosts) {
        this.Count = count;
        this.Hosts = hosts;
    }
}
