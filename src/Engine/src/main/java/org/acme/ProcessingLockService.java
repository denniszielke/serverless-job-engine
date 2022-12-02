package org.acme;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Lock;

@Lock
@ApplicationScoped
public class ProcessingLockService {
    private Boolean isBusy = false;

    @Lock(value = Lock.Type.READ)
    public Boolean isBusy(){
        return isBusy;
    }

    @Lock(value = Lock.Type.WRITE)
    public void setBusy(Boolean newBusyState){
        isBusy = newBusyState;
    }

}
