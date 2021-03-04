/*
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 * This is derived work based on the file
 *  https://github.com/x10-lang/x10/blob/0e5da66f009aa63f6b342f32fba31b870c756f85/apgas/src/apgas/impl/SshLauncher.java
 * licensed under EPL and copyrighted by IBM Corporation 2006-2016.
 *
 * Author of derived work:  Marek Nowicki
 */

package pl.umk.mat.faramir.apgas;

import java.lang.ProcessBuilder.Redirect;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * The {@link SrunLauncher} class implements a launcher using srun.
 * 
 * The implementation is based on {@link apgas.impl.SshLauncher} class
 * that is part of the X10 project (http://x10-lang.org).
 * The file implementing {@link apgas.impl.SshLauncher} is licensed
 * under the Eclipse Public License (EPL):
 *  http://www.opensource.org/licenses/eclipse-1.0.php
 * and copyrighted by IBM Corporation 2006-2016.
 */
public final class SrunLauncher implements apgas.impl.Launcher {
    /**
     * The processes we spawned.
     */
    private final List<Process> processes = new ArrayList<Process>();

    /**
     * Status of the shutdown sequence (0 live, 1 shutting down the Global Runtime, 2 shutting down the JVM).
     */
    private int dying;

    /**
     * Constructs a new {@link SrunLauncher} instance.
     */
    public SrunLauncher() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> terminate()));
    }

    @Override
    public void launch(int n, List<String> command, List<String> hosts, boolean verbose) throws Exception {
        List<String> copy = new ArrayList<>(command);
        final ProcessBuilder pb = new ProcessBuilder(copy);
        pb.redirectOutput(Redirect.INHERIT);
        pb.redirectError(Redirect.INHERIT);
        boolean warningEmitted = false;
        Iterator<String> it = hosts == null ? null : hosts.iterator();
        String host;
        host = it == null ? InetAddress.getLoopbackAddress().getHostAddress()
            : it.next();

        for (int i = 0; i < n; i++) {
            if (it != null) {
                if (it.hasNext()) {
                    host = it.next();
                } else {
                    if (!warningEmitted) {
                        System.err.println(
                                "[APGAS] Warning: hostfile too short; repeating hosts");
                        warningEmitted = true;
                    }
                    it = hosts == null ? null : hosts.iterator();
                    host = it.next();
                }
            }
            Process process;
            boolean local = false;
            try {
                local = InetAddress.getByName(host).isLoopbackAddress();
            } catch (final UnknownHostException e) {
            }
            if (local) {
                process = pb.start();
                if (verbose) {
                    System.err.println(
                            "[APGAS] Spawning new place: " + String.join(" ", command));
                }
            } else {
                copy.addAll(0, Arrays.asList("srun", "-N", "1", "-n", "1", "-w", host));
                copy.addAll(command);
                if (verbose) {
                    System.err.println(
                            "[APGAS] Spawning new place: " + String.join(" ", copy));
                }
                process = pb.start();
                copy.clear();
            }
            synchronized (this) {
                if (dying <= 1) {
                    processes.add(process);
                    process = null;
                }
            }
            if (process != null) {
                process.destroyForcibly();
                throw new IllegalStateException("Shutdown in progress");
            }
        }
    }

    @Override
    public Process launch(List<String> command, String host, boolean verbose) throws Exception {
        List<String> copy = new ArrayList<>(command);
        final ProcessBuilder pb = new ProcessBuilder(copy);
        pb.redirectOutput(Redirect.INHERIT);
        pb.redirectError(Redirect.INHERIT);

        Process process;
        boolean local = false;
        try {
            local = InetAddress.getByName(host).isLoopbackAddress();
        } catch (final UnknownHostException e) {
        }
        if (!local) {
            copy.addAll(0, Arrays.asList("srun", "-N", "1", "-n", "1", "-w", host));
            copy.addAll(command);
        }
        process = pb.start();
        if (verbose) {
            System.err
                .println("[APGAS] Spawning new place: " + String.join(" ", copy));
        }
        if (!local) {
            copy.clear();
        }
        synchronized (this) {
            if (dying <= 1) {
                processes.add(process);
                return process;
            }
        }
        process.destroyForcibly();
        throw new IllegalStateException("Shutdown in progress");
    }

    @Override
    public synchronized void shutdown() {
        if (dying == 0) {
            dying = 1;
        }
    }

    /**
     * Kills all spawned processes.
     */
    private void terminate() {
        synchronized (this) {
            dying = 2;
        }
        for (final Process process : processes) {
            process.destroyForcibly();
        }
    }

    @Override
    public boolean healthy() {
        synchronized (this) {
            if (dying > 0) {
                return false;
            }
        }
        for (final Process process : processes) {
            if (!process.isAlive()) {
                return false;
            }
        }
        return true;
    }
}
