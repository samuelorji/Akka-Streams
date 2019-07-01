package playground

object My extends App {

  sealed trait MyOpt[+A]

  case class MySome[A](elem : A) extends MyOpt[A] {
    def get : A =   this.elem
  }

  case object MyNone extends MyOpt[Nothing]


  def doStuff[B](stuff : B) : MyOpt[B] = {
    try{
      MySome(stuff)
    }catch {
      case ex : Exception =>  MyNone
    }
  }

  println(doStuff(42/5))

}
