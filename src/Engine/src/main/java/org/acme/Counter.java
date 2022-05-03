package org.acme;

import java.util.ArrayList;
import java.util.List;

public class Counter {
    public int Count;
    
    public List<String> Hosts;

    public Counter() {
        this.Count = 0;
        this.Hosts = new ArrayList<String>();
    }
}
