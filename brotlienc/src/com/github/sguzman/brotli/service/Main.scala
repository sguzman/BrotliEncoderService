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
    def afterLast(sep: String) = StringUtils.substringAfterLast(str, sep)
  }

  def extract(url: String) = {
    val user = url.stripPrefix("/").before("/")
    (user, url.stripPrefix(s"/$user/").before("/"))
  }

  val urls: mutable.HashMap[Upload, Boolean] = mutable.HashMap()

  def main(args: Array[String]): Unit = {
    val port = util.Try(System.getenv("PORT").toInt) match {
      case Success(v) => v
      case Failure(_) => 8888
    }

    Server.listen(port) {req =>
      util.Try{
        req.readAs[String].map{url =>
          req.method match {
            case HttpMethod("PUT") =>
              scribe.info(s"Received: $url")
              val obj = new URL(url)
              val (user, repo) = extract(obj.getPath)

              scribe.info(s"Path ${obj.getPath}")
              scribe.info(s"User $user")
              scribe.info(s"Repo $repo")

              if (urls.contains(Upload(user, repo))) {
                Ok("URL already stored")
              } else {

                val exists = Http(s"https://api.github.com/repos/$user/$repo/commits/master").asString
                if (exists.is2xx) {
                  urls.put(Upload(user, repo), false)
                  scribe.info(urls.toString)
                  Ok(urls(Upload(user, repo)).toString)
                } else {
                  NotFound(s"$url not found on github")
                }
              }
            case HttpMethod("GET") =>
              val file = req.path.afterLast("/")
              val branch = req.path.stripSuffix(s"/$file").afterLast("/")
              val repo = req.path.stripSuffix(s"/$branch/$file").afterLast("/")
              val user = req.path.stripSuffix(s"/$repo/$branch/$file").afterLast("/")
              val up = Upload(user, repo)

              if (urls.contains(up)) {
                val body = Ok(Http(s"https://raw.githubusercontent.com/$user/$repo/$branch/$file").asString.body)
                if (urls(up)) {
                  body.addHeaders((HttpString("Accept-Encoding"), HttpString("br")))
                } else {
                  body
                }
              } else {
                NotFound
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
