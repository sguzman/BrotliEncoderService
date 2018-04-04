package com.github.sguzman.brotli.service.typesafe

case class GithubFile(
                     additions: Int,
                     blob_url: String,
                     changes: Int,
                     contents_url: String,
                     deletions: Int,
                     filename: String,
                     raw_url: String,
                     sha: String,
                     status: String
                     )
