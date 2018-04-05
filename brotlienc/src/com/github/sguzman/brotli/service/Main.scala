package com.github.sguzman.brotli.service

import lol.http.{Server, _}
import org.apache.commons.lang3.StringUtils
import scalaj.http.Http

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

    Server.listen(port) {
      case request @ GET at url"/$user/$repo/$branch/$file" =>

        val brotli = request.queryStringParameters.contains("brotli") && request.queryStringParameters("brotli") == "true"
        scribe.info(user)
        scribe.info(repo)
        scribe.info(branch)
        scribe.info(file)
        scribe.info(brotli)

        val resp = Http(s"https://raw.githubusercontent.com/$user/$repo/$branch/$file").asBytes
        if (resp.is2xx) {
          val body = Ok(resp.body)
          val response = if (brotli) {
            body.addHeaders((HttpString("Content-Encoding"), HttpString("br")))
          } else {
            body
          }

          response.addHeaders(
            (HttpString("Access-Control-Allow-Origin"), HttpString("*")),
            (HttpString("Access-Control-Allow-Headers"), HttpString("Origin, X-Requested-With, Content-Type, Accept"))
          )
        } else {
          NotFound
        }
      case _ => NotFound
    }

    println("Listening on http://localhost:8888...")
  }
}
