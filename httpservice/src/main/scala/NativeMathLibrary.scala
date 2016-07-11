package dbraynard.nidum.httpservice

import scala.collection.mutable.ArrayBuffer


object NativeMathLibrary {

  //This method calculates the Fibonacci Sequence from scratch
  //or uses the hint array if passed in.
  def FibonacciSequence(input: Int, hint: Option[ArrayBuffer[BigInt]] = None): ArrayBuffer[BigInt] = {

    //if there is a hint sequence, then return the sequence and
    //then pick up where it left off

    //http://cjwebb.github.io/blog/2013/10/30/fibonacci-numbers-in-scala/


    //When taking from this stream:

    //First the explicit 0 is returned, then second take is from a seed of 1 "inserted" as the start of another stream
    // which is applied to fib current stream, which is just 0, yielding 0,1
    // But, to continue evaluating the stream, the
    // to produce another 1,
    //the third is 1 applied to
    lazy val fibs: Stream[BigInt] = BigInt(0) #:: fibs.scanLeft(BigInt(1))(_+_)

    lazy val fibsWithHint: Stream[BigInt] =
      //last two numbers of the hint array, start scanning with them
      hint.get(hint.get.size-2) #:: fibsWithHint.scanLeft(hint.get.last)(_+_)


    //take from the unbounded stream/enumerable object
    val stream = (hint.isDefined && hint.get.size > 1) match {
      case true => {

        hint.get.toStream.take(hint.get.size-2) #::: fibsWithHint.take(input-hint.get.size+2)

      }
      case false => {
        fibs.take(input)
      }
    }

    //_* indicates to the compiler that a Seq should be used
    val buffer = ArrayBuffer[BigInt](stream: _*)

    buffer

  }

}
