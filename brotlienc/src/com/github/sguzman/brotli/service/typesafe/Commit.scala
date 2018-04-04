package com.github.sguzman.brotli.service.typesafe

case class Commit(
                 author: AuthorSum,
                 comment_count: Int,
                 committer: CommitCommiter,
                 message: String,
                 tree: Tree,
                 url: String
                 )
