package com.github.sguzman.brotli.service

import com.github.sguzman.brotli.service.protoc.upload.Upload
import com.github.sguzman.brotli.service.typesafe.Github
import io.circe.generic.auto._
import io.circe.parser.decode
import lol.http.{Server, _}
import org.apache.commons.lang3.StringUtils
import scalaj.http.Http

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Main {
  final case class Item(user: String, repo: String, branch: String, file: String, brotli: Boolean)

  implicit final class StrWrap(str: String) {
    def before(sep: String) = StringUtils.substringBefore(str, sep)
    def afterLast(sep: String) = StringUtils.substringAfterLast(str, sep)
  }

  def main(args: Array[String]): Unit = {
    val port = util.Try(System.getenv("PORT").toInt) match {
      case Success(v) => v
      case Failure(_) => 8888
    }

    Server.listen(port) {req =>
      util.Try{
        req.method match {
          case HttpMethod("GET") =>
            val split = req.path.split("/")
            val obj = Item(split(0), split(1), split(2), split(3), req.queryStringParameters.contains("brotli"))

            scribe.info(obj.user)
            scribe.info(obj.repo)
            scribe.info(obj.branch)
            scribe.info(obj.file)

            val resp = Http(s"https://raw.githubusercontent.com/${obj.user}/${obj.repo}/${obj.branch}/${obj.file}").asBytes
            if (resp.is2xx) {
              val body = Ok(resp.body)
              val response = if (obj.brotli) {
                body.addHeaders((HttpString("Content-Encoding"), HttpString("br")))
              } else {
                body
              }

              response.addHeaders(
                (HttpString("Access-Control-Allow-Origin"), HttpString("*")),
                (HttpString("Access-Control-Allow-Headers"), HttpString("Origin, X-Requested-With, Content-Type, Accept"))
            } else {
              NotFound
            }
          case _ => NotFound
        }
      } match {
          case Success(v) => v
          case Failure(e) => InternalServerError(e.getMessage)
        }
    }

    println("Listening on http://localhost:8888...")
  }
}
