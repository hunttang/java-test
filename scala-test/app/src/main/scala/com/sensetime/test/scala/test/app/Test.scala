package com.sensetime.test.scala.test.app

/**
  * Created by hunttang on 2/8/17.
  */
class Test(val x: Int) {
  var this.x = x

  def printTest: Unit = {
    println("In Test class")
  }
}

object Test {
  def printTest: Unit = {
    println("In Test Object")
  }
}
