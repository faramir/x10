/*
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 * This is derived work based on the file
 *  https://github.com/faramir/x10/blob/d29984081307901552f5883759ffb1f7b9b51eba/apgas.examples/src/apgas/examples/HelloWorld.java
 * licensed under EPL and copyrighted by IBM Corporation 2006-2016.
 *
 * Author of derived work:  Marek Nowicki
 */
 
 package apgas.examples;

import apgas.Configuration;
import apgas.Place;
import static apgas.Constructs.*;

// $ javac -cp 'target/apgas-2.0.0-SNAPSHOT.jar' Hostnames.java
// $ srun hostname | sort -V > nodes.txt
// $ srun -N 1 -n 1 -w $(head -1 nodes.txt) \
//     java -cp '.:target/apgas-2.0.0-SNAPSHOT.jar:target/lib/*' \
//          -Dapgas.launcher=pl.umk.mat.faramir.apgas.SrunLauncher \
//          -Dapgas.hostfile=nodes.txt \
//          -Dapgas.places=12 \
//          --add-opens=java.base/java.nio=ALL-UNNAMED \
//          --add-opens=java.base/java.lang.invoke=ALL-UNNAMED \
//          --add-opens=java.base/java.io=ALL-UNNAMED \
//          Hostnames

public class Hostnames {
    public static void main(String[] args) {
        System.out.println("Starting app = " + Hostnames.class.getName());
        System.out.println("Passed args  = " + java.util.Arrays.toString(args));
        System.out.println("APGAS_PLACES = " + System.getProperty(Configuration.APGAS_PLACES));

        System.out.println("Running main at " + here() + " of " + places().size() + " places");

        finish(() -> {
            for (Place place : places()) {
                asyncAt(place, () -> {
                    try {
                        System.out.println("Hello from " + here()
                             + " with hostname: " + java.net.InetAddress.getLocalHost().getHostName());
                    } catch (Exception e) {
                        System.out.println("Exception: " + e);
                    }
                });
            }
        });

        System.out.println("Bye from main");
    }
}
