package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class InvestmentRepository(private val dao: InvestmentDao) {
    val allHoldings: Flow<List<Holding>> = dao.getAllHoldings()
    val allTransactions: Flow<List<Transaction>> = dao.getAllTransactions()

    suspend fun insertHolding(holding: Holding) {
        dao.insertHolding(holding)
    }

    suspend fun updateHolding(holding: Holding) {
        dao.updateHolding(holding)
    }

    suspend fun deleteHolding(holding: Holding) {
        dao.deleteHolding(holding)
    }

    suspend fun insertTransaction(transaction: Transaction) {
        dao.insertTransaction(transaction)
    }

    suspend fun clearAll() {
        dao.clearAllHoldings()
        dao.clearAllTransactions()
    }

    suspend fun seedInitialDataIfNeeded() {
        val currentHoldings = dao.getAllHoldings().first()
        if (currentHoldings.isEmpty()) {
            // Seed STOCKS (~$412.5k)
            dao.insertHolding(Holding(symbol = "AAPL", name = "Apple Inc.", quantity = 1200.0, averagePrice = 150.0, category = "STOCKS"))
            dao.insertHolding(Holding(symbol = "GOOG", name = "Alphabet Inc.", quantity = 1000.0, averagePrice = 120.0, category = "STOCKS"))
            dao.insertHolding(Holding(symbol = "TSLA", name = "Tesla Inc.", quantity = 500.0, averagePrice = 225.0, category = "STOCKS"))

            // Seed FUNDS (~$205.2k)
            dao.insertHolding(Holding(symbol = "VTSAX", name = "Vanguard TSM Index", quantity = 1500.0, averagePrice = 105.0, category = "FUNDS"))
            dao.insertHolding(Holding(symbol = "VTIAX", name = "Vanguard Total Intl", quantity = 1000.0, averagePrice = 47.7, category = "FUNDS"))

            // Seed RETIREMENT (~$224.8k)
            dao.insertHolding(Holding(symbol = "RET-401K", name = "Employer Core 401(k)", quantity = 1.0, averagePrice = 150000.0, category = "RETIREMENT"))
            dao.insertHolding(Holding(symbol = "RET-IRA", name = "Target Retirement 2060", quantity = 1000.0, averagePrice = 74.8, category = "RETIREMENT"))

            // Seed some initial transactions so the logs are populated from day one
            dao.insertTransaction(Transaction(symbol = "AAPL", name = "Apple Inc.", quantity = 100.0, price = 145.0, type = "BUY", category = "STOCKS"))
            dao.insertTransaction(Transaction(symbol = "GOOG", name = "Alphabet Inc.", quantity = 50.0, price = 118.0, type = "BUY", category = "STOCKS"))
            dao.insertTransaction(Transaction(symbol = "VTSAX", name = "Vanguard TSM Index", quantity = 200.0, price = 102.5, type = "BUY", category = "FUNDS"))
            dao.insertTransaction(Transaction(symbol = "RET-IRA", name = "Target Retirement 2060", quantity = 100.0, price = 73.0, type = "BUY", category = "RETIREMENT"))
        }
    }
}
