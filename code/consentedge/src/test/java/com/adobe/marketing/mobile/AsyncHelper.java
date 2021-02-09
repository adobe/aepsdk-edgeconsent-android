/* ******************************************************************************
 * ADOBE CONFIDENTIAL
 *  ___________________
 *
 *  Copyright 2021 Adobe
 *  All Rights Reserved.
 *
 *  NOTICE: All information contained herein is, and remains
 *  the property of Adobe and its suppliers, if any. The intellectual
 *  and technical concepts contained herein are proprietary to Adobe
 *  and its suppliers and are protected by all applicable intellectual
 *  property laws, including trade secret and copyright laws.
 *  Dissemination of this information or reproduction of this material
 *  is strictly forbidden unless prior written permission is obtained
 *  from Adobe.
 ******************************************************************************/

package com.adobe.marketing.mobile;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Assert;

public class AsyncHelper {
    private List<String> whitelistedThreads = new ArrayList();

    public AsyncHelper() {
        this.whitelistedThreads.add("pool");
        this.whitelistedThreads.add("ADB");
    }

    public void waitForAppThreads(long timeoutMillis, boolean failOnTimeout) {
        long defaultTimeout = 1000L;
        long defaultSleepTime = 50L;
        long startTime = System.currentTimeMillis();
        long timeoutTestMillis = timeoutMillis > 0L ? timeoutMillis : 1000L;
        long sleepTime = timeoutTestMillis < 50L ? timeoutTestMillis : 50L;
        this.sleep(100L);

        for (Set threadSet = this.getEligibleThreads(); threadSet.size() > 0 && System.currentTimeMillis() - startTime < timeoutTestMillis; threadSet = this.getEligibleThreads()) {
            Iterator var15 = threadSet.iterator();

            while (var15.hasNext()) {
                Thread t = (Thread) var15.next();
                boolean done = false;
                boolean timedOut = false;

                while (true) {
                    while (!done && !timedOut) {
                        if (!t.getState().equals(State.TERMINATED) && !t.getState().equals(State.TIMED_WAITING) && !t.getState().equals(State.WAITING)) {
                            this.sleep(sleepTime);
                            timedOut = System.currentTimeMillis() - startTime > timeoutTestMillis;
                        } else {
                            done = true;
                        }
                    }

                    if (timedOut && failOnTimeout) {
                        Assert.fail(String.format("Timed out waiting for thread %s (%s)", t.getName(), t.getId()));
                    }
                    break;
                }
            }
        }

    }

    private Set<Thread> getEligibleThreads() {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        Set<Thread> eligibleThreads = new HashSet();
        Iterator var3 = threadSet.iterator();

        while (var3.hasNext()) {
            Thread t = (Thread) var3.next();
            if (this.isAppThread(t) && !t.getState().equals(State.WAITING) && !t.getState().equals(State.TERMINATED) && !t.getState().equals(State.TIMED_WAITING)) {
                eligibleThreads.add(t);
            }
        }

        return eligibleThreads;
    }

    private boolean isAppThread(Thread thread) {
        if (thread.isDaemon()) {
            return false;
        } else {
            Iterator var2 = this.whitelistedThreads.iterator();

            String prefix;
            do {
                if (!var2.hasNext()) {
                    return false;
                }

                prefix = (String) var2.next();
            } while (!thread.getName().startsWith(prefix));

            return true;
        }
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException var4) {
            var4.printStackTrace();
        }

    }
}