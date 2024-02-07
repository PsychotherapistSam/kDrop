package de.sam.base.file

import org.tinylog.kotlin.Logger
import java.math.BigInteger
import java.util.regex.Pattern
import kotlin.math.min

// Credit: https://stackoverflow.com/a/48946875/11324248
class FilenameComparator : Comparator<String?> {
    override fun compare(o1: String?, o2: String?): Int {
        // Optional "NULLS LAST" semantics:
        if (o1 == null || o2 == null) return if (o1 == null) if (o2 == null) 0 else -1 else 1

        // Splitting both input strings by the above patterns
        val split1: Array<String> = NUMBERS.split(o1)
        val split2: Array<String> = NUMBERS.split(o2)
        for (i in 0 until min(split1.size, split2.size)) {
            val c1 = split1[i][0]
            val c2 = split2[i][0]
            var cmp = 0

            // If both segments start with a digit, sort them numerically using
            // BigInteger to stay safe
            if (c1.code >= '0'.code && c1.code <= '9'.code && c2.code >= '0'.code && c2.code <= '9'.code) {
                try {
                    cmp = BigInteger(split1[i]).compareTo(BigInteger(split2[i]))
                } catch (e: NumberFormatException) {
                    Logger.error("Error comparing $o1 and $o2")
                }
            }

            // If we haven't sorted numerically before, or if numeric sorting yielded
            // equality (e.g 007 and 7) then sort lexicographically
            if (cmp == 0) {
                cmp = split1[i].compareTo(split2[i])
            }

            // Abort once some prefix has unequal ordering
            if (cmp != 0) {
                return cmp
            }
        }

        // If we reach this, then both strings have equally ordered prefixes, but
        // maybe one string is longer than the other (i.e. has more segments)
        return split1.size - split2.size
    }

    companion object {
        private val NUMBERS: Pattern = Pattern.compile("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)")
    }
}