package org.acme;

public class ProcessingLock {
    private Boolean isBusy = false;

    public Boolean isBusy(){
        return isBusy;
    }

    public void setBusy(Boolean newBusyState){
        isBusy = newBusyState;
    }

}
