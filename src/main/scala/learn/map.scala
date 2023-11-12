package learn

object map_main extends App {
    val times1 = (0 until 9).map(i => i)
    val times2 = (0 until 9).map(i => i*2)
    val times3 = times2.map(t => t*3)
    println(times1)
    println(times2)
    println(times3)
}

