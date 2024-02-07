package de.sam.base.file

/**
 * A sorting comparator to sort strings numerically,
 * ie [1, 2, 10], as opposed to [1, 10, 2].
 */
class NaturalOrderComparator<T>(private val caseInsensitive: Boolean) : Comparator<T> {
    private fun compareRight(a: String, b: String): Int {
        var bias = 0
        var ia = 0
        var ib = 0

        // The longest run of digits wins.  That aside, the greatest
        // value wins, but we can't know that it will until we've scanned
        // both numbers to know that they have the same magnitude, so we
        // remember it in BIAS.
        while (true) {
            val ca = charAt(a, ia)
            val cb = charAt(b, ib)
            if (!Character.isDigit(ca) && !Character.isDigit(cb)) {
                return bias
            } else if (!Character.isDigit(ca)) {
                return -1
            } else if (!Character.isDigit(cb)) {
                return +1
            } else if (ca < cb) {
                if (bias == 0) {
                    bias = -1
                }
            } else if (ca > cb) {
                if (bias == 0) bias = +1
            } else if (ca.code == 0 && cb.code == 0) {
                return bias
            }
            ia++
            ib++
        }
    }

    override fun compare(o1: T, o2: T): Int {
        val a = o1.toString()
        val b = o2.toString()
        var ia = 0
        var ib = 0
        var nza = 0
        var nzb = 0
        var charA: Char
        var charB: Char
        var result: Int
        while (true) {
            // only count the number of zeroes leading the last number compared
            nzb = 0
            nza = nzb
            charA = charAt(a, ia)
            charB = charAt(b, ib)

            // skip over leading zeros
            while (charA == '0') {
                if (charA == '0') {
                    nza++
                } else {
                    // only count consecutive zeroes
                    nza = 0
                }

                // if the next character isn't a digit, then we've had a run of only zeros
                // we still need to treat this as a 0 for comparison purposes
                if (!Character.isDigit(charAt(a, ia + 1))) break
                charA = charAt(a, ++ia)
            }
            while (charB == '0') {
                if (charB == '0') {
                    nzb++
                } else {
                    // only count consecutive zeroes
                    nzb = 0
                }

                // if the next character isn't a digit, then we've had a run of only zeros
                // we still need to treat this as a 0 for comparison purposes
                if (!Character.isDigit(charAt(b, ib + 1))) break
                charB = charAt(b, ++ib)
            }

            // process run of digits
            if (Character.isDigit(charA) && Character.isDigit(charB)) {
                if (compareRight(
                        a.substring(ia), b
                            .substring(ib)
                    ).also { result = it } != 0
                ) {
                    return result
                }
            }
            if (charA.code == 0 && charB.code == 0) {
                // The strings compare the same.  Perhaps the caller
                // will want to call strcmp to break the tie.
                return nza - nzb
            }
            if (charA < charB) {
                return -1
            } else if (charA > charB) {
                return +1
            }
            ++ia
            ++ib
        }
    }

    private fun charAt(s: String, i: Int): Char {
        return if (i >= s.length) {
            0.toChar()
        } else {
            if (caseInsensitive) s[i].uppercaseChar() else s[i]
        }
    }

    companion object {
        val NUMERICAL_ORDER: Comparator<String> = NaturalOrderComparator(false)
        val CASEINSENSITIVE_NUMERICAL_ORDER: Comparator<String> = NaturalOrderComparator(true)
    }
}