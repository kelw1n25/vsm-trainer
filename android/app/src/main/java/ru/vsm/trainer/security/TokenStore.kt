package ru.vsm.trainer.security

import ru.vsm.trainer.data.remote.dto.SessionTokens

/** Где лежит сессия. В приложении — зашифровано ключом Android Keystore, в тестах — память. */
interface TokenStore {
    fun load(): SessionTokens?
    fun save(tokens: SessionTokens)
    fun clear()
}

class InMemoryTokenStore(private var tokens: SessionTokens? = null) : TokenStore {
    @Synchronized override fun load() = tokens
    @Synchronized override fun save(tokens: SessionTokens) { this.tokens = tokens }
    @Synchronized override fun clear() { tokens = null }
}
