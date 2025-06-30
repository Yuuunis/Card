package com.example.cardgame.game

data class Player(
    val id: Int,
    var hand: MutableList<Card>,
    var totalScore: Int = 0,
    var bid: Int = 0,
    var tricksWon: Int = 0
)