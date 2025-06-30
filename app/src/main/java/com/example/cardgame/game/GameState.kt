package com.example.cardgame.game

data class GameState(
    val players: List<Player>,
    var deck: MutableList<Card>,
    var currentPlayerIndex: Int,
    var currentTrick: MutableList<Card>,
    var trumpSuit: Suit,
    var biddingPhase: Boolean = true,
    var roundNumber: Int = 1
)