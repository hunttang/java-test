package com.sensetime.test.scala.test.app

import java.util.Date

/**
  * Created by Hunt Tang on 1/24/17.
  */
object Main {
  def main(args: Array[String]): Unit = {
    val date: Date = new Date(1487670523000L);
    println(date)
    val test = new Test(0)
    test.printTest
    Test.printTest
  }
}
