package com.example.cardgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cardgame.game.Card
import com.example.cardgame.game.GameViewModel
import com.example.cardgame.game.Player
import com.example.cardgame.ui.theme.CardGameTheme

class MainActivity : ComponentActivity() {
    private val gameViewModel by viewModels<GameViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CardGameTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameScreen(gameViewModel)
                }
            }
        }
    }
}

@Composable
fun GameScreen(gameViewModel: GameViewModel) {
    val gameState = gameViewModel.gameState

    if (gameState != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Scoreboard(players = gameState.players)

            // Top player
            PlayerUi(player = gameState.players[2])

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left player
                PlayerUi(player = gameState.players[1])

                // Trick
                TrickView(trick = gameState.currentTrick)

                // Right player
                PlayerUi(player = gameState.players[3])
            }

            // Bottom player (user)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (gameState.biddingPhase && gameState.players[0].bid == 0) {
                    BiddingView(onBid = { bid -> gameViewModel.placeBid(0, bid) })
                }
                PlayerUi(player = gameState.players[0], isUser = true, onCardClick = {
                    if (!gameState.biddingPhase) {
                        gameViewModel.playCard(it)
                    }
                })
            }
        }
    }
}

@Composable
fun Scoreboard(players: List<Player>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for (player in players) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Player ${player.id}")
                Text(text = "Score: ${player.totalScore}")
                Text(text = "Bid: ${player.bid} / Won: ${player.tricksWon}")
            }
        }
    }
}

@Composable
fun PlayerUi(player: Player, isUser: Boolean = false, onCardClick: (Card) -> Unit = {}) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Player ${player.id}")
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(player.hand) { card ->
                CardView(card = card, isUser = isUser, onClick = { onCardClick(card) })
            }
        }
    }
}

@Composable
fun CardView(card: Card, isUser: Boolean, onClick: () -> Unit) {
    val cardText = if (isUser) {
        "${card.rank.name.first()}${card.suit.name.first()}"
    } else {
        "\uD83C\uDCA0" // Unicode for a playing card back
    }
    Button(onClick = onClick, enabled = isUser) {
        Text(text = cardText)
    }
}

@Composable
fun TrickView(trick: List<Card>) {
    Row(
        modifier = Modifier
            .size(120.dp)
            .background(Color.LightGray)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (card in trick) {
            Text(text = "${card.rank.name.first()}${card.suit.name.first()}", fontSize = 20.sp)
        }
    }
}

@Composable
fun BiddingView(onBid: (Int) -> Unit) {
    var bid by remember { mutableStateOf(1) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Place your bid:")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { if (bid > 1) bid-- }) { Text(text = "-") }
            Text(text = "$bid", modifier = Modifier.padding(horizontal = 16.dp))
            Button(onClick = { if (bid < 13) bid++ }) { Text(text = "+") }
        }
        Button(onClick = { onBid(bid) }) {
            Text(text = "Submit Bid")
        }
    }
}
