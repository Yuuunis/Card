package com.example.cardgame.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    var gameState by mutableStateOf<GameState?>(null)
        private set

    private val viewModelScope = CoroutineScope(Dispatchers.Default)

    init {
        startGame()
    }

    private fun startGame() {
        val deck = createDeck().toMutableList()
        deck.shuffle()

        val players = List(4) { Player(it, mutableListOf()) }
        dealCards(deck, players)

        gameState = GameState(
            players = players,
            deck = deck,
            currentPlayerIndex = 0,
            currentTrick = mutableListOf(),
            trumpSuit = Suit.HEARTS // Hearts are always trump
        )
        // Start bidding for computer players
        viewModelScope.launch {
            computerBid()
        }
    }

    private fun createDeck(): List<Card> {
        return Suit.values().flatMap { suit ->
            Rank.values().map { rank ->
                Card(suit, rank)
            }
        }
    }

    private fun dealCards(deck: MutableList<Card>, players: List<Player>) {
        for (player in players) {
            player.hand.clear()
            player.tricksWon = 0
            player.bid = 0
        }
        // Make sure the deck is full before dealing
        if (deck.size < 52) {
            deck.addAll(createDeck().filterNot { deck.contains(it) })
            deck.shuffle()
        }

        for (i in 0 until 13) {
            for (player in players) {
                if (deck.isNotEmpty()) {
                    player.hand.add(deck.removeAt(0))
                }
            }
        }
    }

    fun placeBid(playerIndex: Int, bid: Int) {
        val currentState = gameState ?: return
        if (playerIndex < 0 || playerIndex >= currentState.players.size) return

        val player = currentState.players[playerIndex]
        player.bid = bid

        val allBidsPlaced = currentState.players.all { it.bid != 0 }

        if (allBidsPlaced) {
            gameState = currentState.copy(biddingPhase = false)
            playComputerTurns()
        } else if (playerIndex == 0) { // If the user has placed a bid, trigger the next computer bid
            viewModelScope.launch {
                computerBid()
            }
        }
    }


    private suspend fun computerBid() {
        val currentState = gameState ?: return
        for (i in 1..3) {
            val player = currentState.players[i]
            if (player.bid == 0) {
                delay(1000) // Simulate thinking
                // Simple AI: bid based on number of high cards and trump cards
                val highCards = player.hand.count { it.rank.value >= Rank.QUEEN.value || it.suit == currentState.trumpSuit }
                val calculatedBid = highCards.coerceIn(1, 5)
                placeBid(i, calculatedBid)
                break // only one computer bids at a time
            }
        }
    }


    fun playCard(card: Card) {
        val currentState = gameState ?: return
        val playerIndex = currentState.currentPlayerIndex
        val player = currentState.players[playerIndex]

        if (player.id != 0 && !isValidMove(card, player.hand, currentState.currentTrick)) {
             // This is a computer move, and it should always be valid.
             // If not, there is a bug in the AI or validation logic.
             // For now, we'll just let it pass to avoid getting stuck.
        } else if (player.id == 0 && !isValidMove(card, player.hand, currentState.currentTrick)) {
            // Optionally, provide feedback to the user about the invalid move
            return
        }


        player.hand.remove(card)
        val newTrick = currentState.currentTrick.toMutableList()
        newTrick.add(card)


        val nextPlayerIndex = (playerIndex + 1) % 4

        gameState = currentState.copy(
            currentTrick = newTrick,
            currentPlayerIndex = nextPlayerIndex
        )


        if (newTrick.size == 4) {
            // End of trick
            viewModelScope.launch {
                delay(1500) // Pause to see the full trick
                endTrick()
            }
        } else {
            // Next player's turn
            playComputerTurns()
        }
    }

    private fun isValidMove(card: Card, hand: List<Card>, trick: List<Card>): Boolean {
        if (trick.isEmpty()) {
            return true // Can play any card to start a trick
        }
        val leadingSuit = trick.first().suit
        val hasLeadingSuit = hand.any { it.suit == leadingSuit }
        if (hasLeadingSuit) {
            if (card.suit != leadingSuit) {
                // This is the penalty condition from rule 4
                val player = gameState!!.players[gameState!!.currentPlayerIndex]
                player.totalScore -= 3
                return true // Allow the move but apply penalty
            }
            return card.suit == leadingSuit
        }
        return true // Can play any card if you don't have the leading suit
    }

    private fun endTrick() {
        val currentState = gameState ?: return
        val trick = currentState.currentTrick
        val leadingSuit = trick.first().suit
        val trumpSuit = currentState.trumpSuit

        var winningCard = trick.first()
        var winnerIndexInTrick = 0

        for (i in 1 until trick.size) {
            val card = trick[i]
            if (card.suit == winningCard.suit) {
                if (card.rank.value > winningCard.rank.value) {
                    winningCard = card
                    winnerIndexInTrick = i
                }
            } else if (card.suit == trumpSuit && winningCard.suit != trumpSuit) {
                winningCard = card
                winnerIndexInTrick = i
            }
        }

        // The player who started the trick is `(currentPlayerIndex - 3 + 4) % 4`
        // but since we update currentPlayerIndex before calling endTrick, it is now the player *after* the last one who played.
        // So the first player of the trick is `(currentState.currentPlayerIndex - 4 + 4) % 4` which is `currentState.currentPlayerIndex`
        // This seems overly complicated. Let's rethink.
        // The last player to play is (currentPlayerIndex - 1 + 4) % 4.
        // The trick contains cards in the order they were played.
        // The first player of the trick is the one who is `(currentPlayerIndex - 4 + 4) % 4` if we are at the end of the trick.
        // Let's find the starting player of the trick.
        val lastPlayerIndex = (currentState.currentPlayerIndex - 1 + 4) % 4
        val firstPlayerOfTheTrick = (lastPlayerIndex - 3 + 4) % 4

        val winnerIndex = (firstPlayerOfTheTrick + winnerIndexInTrick) % 4
        val winner = currentState.players[winnerIndex]
        winner.tricksWon++

        if (currentState.players.all { it.hand.isEmpty() }) {
            // End of round
            endRound()
        } else {
            // Start new trick, winner leads
            gameState = currentState.copy(
                currentTrick = mutableListOf(),
                currentPlayerIndex = winnerIndex
            )
            playComputerTurns()
        }
    }

    private fun endRound() {
        val currentState = gameState ?: return
        // Update scores
        for (player in currentState.players) {
            if (player.tricksWon >= player.bid) {
                player.totalScore += player.bid
            } else {
                player.totalScore -= player.bid
            }
        }

        if (currentState.players.any { it.totalScore >= 30 }) {
            // Game over
            // For now, just restart the game
            startGame()
        } else {
            // Start new round
            val deck = createDeck().toMutableList()
            deck.shuffle()
            dealCards(deck, currentState.players)
            val nextRoundNumber = currentState.roundNumber + 1
            gameState = GameState(
                players = currentState.players,
                deck = deck,
                currentPlayerIndex = (currentState.roundNumber) % 4, // Next player starts
                currentTrick = mutableListOf(),
                trumpSuit = Suit.HEARTS,
                biddingPhase = true,
                roundNumber = nextRoundNumber
            )
            viewModelScope.launch {
                computerBid()
            }
        }
    }

    private fun playComputerTurns() {
        viewModelScope.launch {
            var currentPlayerIndex = gameState?.currentPlayerIndex ?: return@launch
            while (currentPlayerIndex != 0 && gameState?.biddingPhase == false) {
                delay(1000) // Simulate thinking
                val player = gameState!!.players[currentPlayerIndex]
                val trick = gameState!!.currentTrick
                val validMoves = player.hand.filter { isValidMove(it, player.hand, trick) }
                if (validMoves.isNotEmpty()) {
                    // Simple AI: play a random valid card
                    val cardToPlay = validMoves.random()
                    playCard(cardToPlay)
                }
                // playCard will update the currentPlayerIndex, so we need to get the new one
                val newGameState = gameState
                if (newGameState != null && newGameState.currentTrick.size < 4) {
                     currentPlayerIndex = newGameState.currentPlayerIndex
                } else {
                    // trick is full, break the loop
                    break
                }
            }
        }
    }
}