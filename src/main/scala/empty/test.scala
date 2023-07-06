package empty

object GlobalStrings {
  val sharedString: String = "This is a shared string"
}

class MyClass {
  import GlobalStrings.sharedString

  def printSharedString(): Unit = {
    println(sharedString)
  }
}

object AnotherObject {
//   import GlobalStrings.sharedString

  def printSharedString(): Unit = {
    // println(sharedString)
    println(GlobalStrings.sharedString)
  }
}

object main extends App {
    // Usage
//     val myObject = new MyClass()
//     myObject.printSharedString()
// 
//     AnotherObject.printSharedString()

}

