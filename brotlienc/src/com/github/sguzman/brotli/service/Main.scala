package com.github.sguzman.brotli.service

import java.net.URL

import com.github.sguzman.brotli.service.protoc.upload.Upload
import com.github.sguzman.brotli.service.typesafe.Github
import lol.http.{Server, _}
import org.apache.commons.lang3.StringUtils
import scalaj.http.Http

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import io.circe.parser.decode
import io.circe.generic.auto._

object Main {
  implicit final class StrWrap(str: String) {
    def before(sep: String) = StringUtils.substringBefore(str, sep)
    def afterLast(sep: String) = StringUtils.substringAfterLast(str, sep)
  }

  def extract(url: String) = {
    val user = url.stripPrefix("/").before("/")
    (user, url.stripPrefix(s"/$user/").before("/"))
  }

  val urls: mutable.HashSet[Upload] = mutable.HashSet()

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
              val up = Upload(user, repo)

              scribe.info(s"Path ${obj.getPath}")
              scribe.info(s"User $user")
              scribe.info(s"Repo $repo")

              if (urls.contains(up)) {
                Ok("URL already stored")
              } else {

                val exists = Http(s"https://api.github.com/repos/$user/$repo/commits/master").asString
                if (exists.is2xx && decode[Github](exists.body).right.get.author.`type` != "") {
                  urls.add(up)
                  scribe.info(urls.toString)
                  Ok(urls(up).toString)
                } else {
                  NotFound(s"$url not found on github")
                }
              }
            case HttpMethod("GET") =>
              val query = req.queryString.getOrElse("")

              val path = req.path.stripSuffix(query)
              val file = req.path.afterLast("/")
              val branch = req.path.stripSuffix(s"/$file").afterLast("/")
              val repo = req.path.stripSuffix(s"/$branch/$file").afterLast("/")
              val user = req.path.stripSuffix(s"/$repo/$branch/$file").afterLast("/")
              val up = Upload(repo, user)

              if (urls.contains(up)) {
                val body = Ok(Http(s"https://raw.githubusercontent.com/$user/$repo/$branch/$file").asBytes.body)
                if (query == "brotli=true") {
                  body.addHeaders((HttpString("Content-Encoding"), HttpString("br")))
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
