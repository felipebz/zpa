package org.sonar.plsqlopen.squid;

import java.util.Collection;
import java.util.Iterator;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class ProgressReport implements Runnable {

    private final long period;
    private final Logger logger;
    private int count;
    private int currentFileNumber = -1;
    private InputFile currentFile;
    private Iterator<InputFile> it;
    private final Thread thread;
    private final String adjective;
    private boolean success = false;

    public ProgressReport(String threadName, long period, Logger logger, String adjective) {
        this.period = period;
        this.logger = logger;
        this.adjective = adjective;
        thread = new Thread(this);
        thread.setName(threadName);
        thread.setDaemon(true);
    }

    public ProgressReport(String threadName, long period, String adjective) {
        this(threadName, period, Loggers.get(ProgressReport.class), adjective);
    }

    public ProgressReport(String threadName, long period) {
        this(threadName, period, "analyzed");
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                Thread.sleep(period);
                synchronized (this) {
                    log(currentFileNumber + "/" + count + " files " + adjective + ", current file: "
                            + currentFile.relativePath());
                }
            } catch (InterruptedException e) {
                break;
            }
        }
        synchronized (this) {
            if (success) {
                log(count + "/" + count + " source files have been " + adjective);
            }
        }
    }

    public synchronized void start(Collection<InputFile> files) {
        count = files.size();
        it = files.iterator();

        nextFile();

        log(count + " source files to be " + adjective);
        thread.start();
    }

    public synchronized void nextFile() {
        if (it.hasNext()) {
            currentFileNumber++;
            currentFile = it.next();
        }
    }

    public synchronized void stop() {
        success = true;
        thread.interrupt();
    }

    public synchronized void cancel() {
        thread.interrupt();
    }

    public void join() throws InterruptedException {
        thread.join();
    }

    private void log(String message) {
        synchronized (logger) {
            logger.info(message);
            logger.notifyAll();
        }
    }

}
