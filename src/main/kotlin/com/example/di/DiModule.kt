package com.example.di

import com.example.state.GameState
import com.example.state.managers.network.GameService
import com.example.state.managers.network.WebSocketConnectionManager
import com.example.state.managers.network.ConnectionManager
import org.koin.dsl.module

val appModule = module {
    single { GameState() }
    single<ConnectionManager> { WebSocketConnectionManager() }
    single { GameService(get(), get()) }
}
