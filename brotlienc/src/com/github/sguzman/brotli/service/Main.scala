package com.github.sguzman.brotli.service

import java.net.URL

import com.github.sguzman.brotli.service.protoc.upload.Upload
import lol.http.{Server, _}
import org.apache.commons.lang3.StringUtils
import scalaj.http.Http

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Main {
  implicit final class StrWrap(str: String) {
    def before(sep: String) = StringUtils.substringBefore(str, sep)
  }

  val urls: mutable.HashMap[String, Upload] = mutable.HashMap()

  def main(args: Array[String]): Unit = {
    val port = util.Try(System.getenv("PORT").toInt) match {
      case Success(v) => v
      case Failure(_) => 8888
    }

    Server.listen(port) {req =>
      util.Try{
        req.readAs[String].map { url =>
          scribe.info(s"Received: $url")
          val obj = new URL(url)
          req.method match {
            case HttpMethod("PUT") =>
              scribe.info(s"Path ${obj.getPath}")

              val user = obj.getPath.stripPrefix("/").before("/")
              scribe.info(s"User $user")

              val repo = obj.getPath.stripPrefix(s"/$user/").before("/")
              scribe.info(s"Repo $repo")

              val file = StringUtils.substringAfterLast(obj.getPath, "/")
              scribe.info(s"File $file")

              val exists = Http(s"https://api.github.com/repos/$user/$repo/commits/master").asString
              if (exists.is2xx) {
                urls.put(url, Upload(s"/$user/$repo/blob/$file"))
                Ok(urls(url).toProtoString)
              } else {
                NotFound(s"$url not found on github")
              }
            case _ => NotFound
          }
        }
      } match {
          case Success(v) => v
          case Failure(e) => InternalServerError(e.getMessage)
        }
    }

    println("Listening on http://localhost:8888...")
  }
}
