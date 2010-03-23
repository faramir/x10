/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2010.
 */

import harness.x10Test;
	
public class RefB(f2: RefA) extends x10Test {
	public def this(f2: RefA) { 
		this.f2=f2;
	}
	
	public def run(): boolean { return true;}
}
