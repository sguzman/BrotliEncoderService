package com.github.sguzman.brotli.service

import scala.collection.mutable

object Main {
  final case class Model(url: String, isBrotli: Boolean)
  val urls: mutable.HashSet[Model] = mutable.HashSet()

  def main(args: Array[String]): Unit = {
    println("Hello world")
  }
}
