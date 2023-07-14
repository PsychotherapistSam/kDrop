package de.sam.base.utils

import org.joda.time.DateTime
import java.util.*

class GitCommitInfo {
    companion object {
        val gitProperties = Properties().apply {
            val gitPropsInputStream = Thread.currentThread().contextClassLoader.getResourceAsStream("git.properties")
            if (gitPropsInputStream != null) {
                this.load(gitPropsInputStream)
            }
        }
        val gitCommitId = gitProperties.getProperty("git.commit.id.abbrev", "unknown")
        val gitCommitTime = DateTime.parse(gitProperties.getProperty("git.commit.time", "1970-01-01T00:00:00+0000"))
    }

}