package graphs

import scala.collection.immutable.{ListMap, TreeMap}


//  object MyClass {
//    private var num = 100
//
//    def main(args: Array[String]): Unit = {
//      val mc = MyClass(num)
//      mc.add(5)
//      println(mc.num)
//    }
//  }
//
//  case class MyClass(num: Int) {
//    def add(num: Int): Unit = {
//      MyClass.num = num * 10
//    }
//
//  }

object y extends App {

//  def nonRepeated(word : String) : Char = {
//    val wordLower =  word.toLowerCase
//    if(wordLower.tail.contains(wordLower.head)) nonRepeated(wordLower.tail.toString)
//    else wordLower.head
//  }
//
//  println(nonRepeated("SalesWings"))
//
//  val part_1 = Array[String]("   9: dog    ", "  2  :quick   ", "4:       fox", " 7:  the   ", "5:  jumps")
//  val part_2 = Array[String]("3    :brown", " 1  :   the", "  6:over", "8   :lazy", "10:   this", "   11  :  morning")
//
//  def combineArrays(arr1 : Array[String],arr2 : Array[String]) : String  = {
//   val arrangedMap =  (arr1 ++ arr2).foldLeft(TreeMap[Int , String]()){
//     case (map, entry) =>
//       val entryArray = entry.split(":")
//       map.updated(entryArray.head.trim.toInt, entryArray.last.trim)
//   }
//    arrangedMap.values.mkString(" ")
//  }
//  println(combineArrays(part_1,part_2))


  var task  : Runnable = null

  var runningThread : Thread   = new Thread(() => {
    while(true){
      while(task == null){
        runningThread.synchronized {
          println("[background] Waiting for a task .....  ")
          runningThread.wait()
        }
      }

      task.synchronized{
        println("[background] I have a task......")
        task.run()
        task = null
      }
    }
  })

  private def delegateTaskToBackgroundThread(r : Runnable) = {
    if(task == null) task = r
    runningThread.synchronized{
      runningThread.notify()
    }
  }

  runningThread.start()

  Thread.sleep(1000)
  delegateTaskToBackgroundThread(() => println(42))

  Thread.sleep(1000)
  delegateTaskToBackgroundThread(() => println(42))





}

