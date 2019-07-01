package playground

import scala.collection.immutable.Stack

object test extends App {
//  def getMaxBinaryGap(number: Int): Int = {
//    def doBinary(acc: List[Int], num: Int): List[Int] = {
//      num match {
//        case 0 | 1 => num :: acc
//        case _ => doBinary((num % 2) :: acc, num / 2)
//      }
//    }
//
//    val binaryAsString = doBinary(List[Int](), number).mkString
//    val croppedZeros = "[0]+$".r.replaceFirstIn("^[0]+".r.replaceFirstIn(binaryAsString, ""), "")
//
//    "[0]{1,}".r.findAllMatchIn(croppedZeros).toStream match {
//      case stream if !stream.isEmpty => stream.map(_.toString).map(_.length).max
//      case _ => 0
//    }
//  }
//
//  // println(getMaxBinaryGap(19))
//  def getodd(arr: Array[Int]): Int = {
//    val array = arr.sorted
//    if (arr.length < 2) arr(0)
//    else if (array(0) == arr(1)) getodd(array.drop(2))
//    else array(0)
//  }
//
//  //  def solution(a: Array[Int]): Int = {
//  //    // write your code in Scala 2.12
//  //    val array = a.sorted
//  //    def getodd(arr : Array[Int], index : Int ) : Int = {
//  //      try{
//  //        if(arr.length < 2) arr(index)
//  //        else if(arr(index) == arr(index + 1)) getodd(arr,index + 2)
//  //        else arr(index)}
//  //      catch {
//  //        case e : ArrayIndexOutOfBoundsException => arr.last
//  //      }
//  //    }
//  //    getodd(array, 0)
//  //  }
//
////  def solution(a: Array[Int]): Int = {
////    // write your code in Scala 2.12
////    val array = a.sorted
////    val arr1 = array.take(array.length / 2 + 1)
////    val arr2 = array.drop(array.length / 2 + 1)
////    //    println(arr1.mkString ); println(arr2.mkString)
////    //    def getOdd(arr : Array[Int], index : Int, checked1 : Boolean = false) : Int = {
////    //      if(index > arr.length - 2) arr.last
////    //      else if(arr(index) == arr(index + 1)) getOdd(arr,index + 2)
////    //      else arr(index)
////    //    }
//    //    if(arr1.length % 2 == 0){
//    //      getOdd(arr2,0)
//    //    }else{
//    //      getOdd(arr1,0)
//    //    }
//    //private var ind : Int = 0
//    arr1.reduce((x, y) => {
//      if (x == y) 0 else -1
//    })
//
//
//  }

  //  val go = new PartialFunction[Array[Int],Int]{
  //    override def isDefinedAt(x: Array[Int]): Boolean =
  //
  //    override def apply(v1: Array[Int]): Int = go()
  //  }

  /**
    * split the array into 2 .... if the first is odd in length
    * check if the first.last == second.head .....if true loop on second array..but stop check
    * loop through first and then
    *
    *
    */
  //  println(solution(Array(1,1,2,2,3,3,4)))
  //  println("*" * 20)
  //  println(solution(Array(9, 3, 9, 3, 9, 7, 9)))
  //def solutio(x: Int, y: Int, d: Int): Int  = if((y-x) % d ==0) (y-x) / d else (y -x)/d + 1
  //  println(solutio(10,85,30) )

  //  def getLowestNum(n : Int) : Int = {
  //  val length_num = n.toString.length
  //  Math.pow(10,length_num - 1).toInt
  //


  //
  //  def solution(n: Int): Int = {
  //    // write your code in Scala 2.12
  //    if(n <= 1){
  //      0
  //    }else{
  //      val length_num = n.toString.length
  //      Math.pow(10,length_num - 1).toInt
  //
  //    }
  //  }
  //
  //  println(solution(99992))
  import scala.util.control.Breaks._

  def solution(a: Int, b: Int): Int = {
    // write your code in Scala 2.12
    multiply(0,b,0,a to b)

  }

  def multiply(x : Int, y : Int ,count  : Int, range : Range) : Int  = {
        if(x * (x + 1) > y) count
        else if(range.contains(x * (x + 1))) multiply(x + 1, y, count + 1, getAccceptableRange(x,range))
        else multiply(x + 1, y, count, getAccceptableRange(x,range))

  }
  def getAccceptableRange(ind : Int , rng : Range ) : Range = {
    if(ind * (ind + 1) > rng.head) rng.tail else rng
  }


  println(solution(0,2))
}

