/*
 *
 * (C) Copyright IBM Corporation 2006
 *
 *  This file is part of X10 Test.
 *
 */
//LIMITATION:
//BadPlaceExceptions not being thrown correctly.
import harness.x10Test;

public class PlaceCast extends x10Test {
	var nplaces: int = 0;

	public def run(): boolean = {
		val d: Dist = Dist.makeUnique(Place.places);
		x10.io.Console.OUT.println("num places = " + Place.MAX_PLACES);
		val disagree: Array[BoxedBoolean]{dist==d} 
		= Array.makeVar[BoxedBoolean](d, ((p): Point): BoxedBoolean => {
				x10.io.Console.OUT.println("The currentplace is:" + here);
				return new BoxedBoolean();
			});
		finish ateach ((p) in d) {
			// remember if here and d[p] disagree
			// at any activity at any place
			try {
				val q: Place = d(p).next();
				var x: BoxedBoolean = disagree(p) as (BoxedBoolean!q);
				at (this) { atomic { nplaces++; } }
			} catch (var x: BadPlaceException)  {
				x10.io.Console.OUT.println("Caught bad place exception for " + p);
			}
		}
		x10.io.Console.OUT.println("nplaces == " + nplaces);
		return nplaces == 0;
	}

	public static def main(var args: Rail[String]): void = {
		new PlaceCast().execute();
	}

	static class BoxedBoolean {
		var v: boolean = false;
	}
}
