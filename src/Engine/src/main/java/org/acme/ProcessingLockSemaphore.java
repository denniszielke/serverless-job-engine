package org.acme;

import java.util.concurrent.Semaphore;

public class ProcessingLockSemaphore extends ProcessingLock {
    private Semaphore mutex = new Semaphore(1);

    @Override
    public void setBusy(Boolean newBusyState){
        try {
            mutex.acquire();
            super.setBusy(newBusyState);
        } catch (InterruptedException e) {
            // exception handling code
        } finally {
            mutex.release();
        }
    }

}
