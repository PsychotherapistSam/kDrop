package de.sam.base.utils

import dev.samstevens.totp.code.CodeGenerator
import dev.samstevens.totp.code.CodeVerifier
import dev.samstevens.totp.code.DefaultCodeGenerator
import dev.samstevens.totp.code.DefaultCodeVerifier
import dev.samstevens.totp.time.SystemTimeProvider
import dev.samstevens.totp.time.TimeProvider

private val timeProvider: TimeProvider = SystemTimeProvider()
private val codeGenerator: CodeGenerator = DefaultCodeGenerator()
val verifier: CodeVerifier = DefaultCodeVerifier(codeGenerator, timeProvider)
