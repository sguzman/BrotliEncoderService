package com.github.sguzman.brotli.service

import lol.http.Server

import scala.collection.mutable
import scala.util.{Failure, Success}
import lol.http._
import scalaj.http.{Http, HttpOptions}

import scala.concurrent.ExecutionContext.Implicits.global

object Main {
  final case class Model(url: String, isBrotli: Boolean)
  val urls: mutable.HashMap[String, Model] = mutable.HashMap()

  def main(args: Array[String]): Unit = {
    val port = util.Try(System.getenv("PORT").toInt) match {
      case Success(v) => v
      case Failure(_) => 8888
    }

    Server.listen(port) {req =>
      val url = req.readAs[Array[Byte]].map(_.map(_.toChar).mkString)
      req.method match {
        case HttpMethod("PUT") =>
          Ok(Http(url.toString).option(HttpOptions.followRedirects(true)).asBytes.is2xx.toString)
        case _ => NotFound
      }
    }

    println("Listening on http://localhost:8888...")
  }
}
