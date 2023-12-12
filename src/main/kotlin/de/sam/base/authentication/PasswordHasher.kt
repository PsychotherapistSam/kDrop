package de.sam.base.authentication

import com.password4j.Argon2Function
import com.password4j.Password
import com.password4j.types.Argon2
import de.sam.base.config.Configuration
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PasswordHasher : KoinComponent {
    private val argon2Instance: Argon2Function = Argon2Function.getInstance(15360, 3, 2, 32, Argon2.ID, 19)
    private val config: Configuration by inject()

    fun hashPassword(password: String, salt: String): String {
        return Password.hash(password)
            .addSalt(salt)
            .addPepper(config.passwordPepper)
            .with(argon2Instance)
            .result
    }

    fun verifyPassword(password: String, hashedPassword: String, salt: String): Boolean {
        return Password.check(password, hashedPassword)
            .addSalt(salt)
            .addPepper(config.passwordPepper)
            .with(argon2Instance)
    }
}