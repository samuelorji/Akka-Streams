package playground

import scala.collection.mutable

object My extends App {


//(1 to 10)(_ : Int => println("Hello"))

  /**
    * Let's implement bubble sort
    */

  val unsortedArray =  mutable.ArrayBuffer[Int](1,4,3,2,7,5,6,0,9,8)

  def swap(unsortedArray: mutable.ArrayBuffer[Int], j: Int) = {
    val temp = unsortedArray(j)
    unsortedArray(j) = unsortedArray(j + 1)
    unsortedArray(j + 1) = temp

  }

  for(i <- (0 until unsortedArray.length) ){
    for(j <- (0 until unsortedArray.length - i -1)){
      if(unsortedArray(j) >  unsortedArray(j + 1)){
        swap(unsortedArray,j)
      }
    }
  }
  println(unsortedArray)
}
