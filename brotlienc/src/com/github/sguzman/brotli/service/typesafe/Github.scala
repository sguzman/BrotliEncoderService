package com.github.sguzman.brotli.service.typesafe

case class Github(
                   author: Author,
                   comments_url: String,
                   commit: Commit,
                   committer: Committer,
                   files: List[GithubFile],
                   html_url: String,
                   parents: List[Parent],
                   sha: String,
                   stats: Stat,
                   url: String
                 )
