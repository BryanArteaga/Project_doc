package com.example.doc.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.doc.R
import com.example.doc.navigation.Routes
import com.example.doc.utils.isLandscape

@Composable
fun WelcomeScreen(navController: NavController, onUiReady: () -> Unit) {

    LaunchedEffect(Unit) {
        android.util.Log.d("WelcomeScreen", "WelcomeScreen rendered, calling onUiReady")
        onUiReady()
    }
    val isLandscape = isLandscape()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_2),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            ResponsiveWelcomeCard(
                navController = navController,
                isLandscape = isLandscape
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ResponsiveWelcomeCard(
    navController: NavController,
    isLandscape: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(if (isLandscape) 32.dp else 32.dp),
            elevation = CardDefaults.cardElevation(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isLandscape) Modifier.widthIn(max = 700.dp)
                    else Modifier
                )
        ) {
            if (isLandscape) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Bienvenido a DocControl",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Controla y mejora tu salud de forma sencilla",
                            fontSize = 18.sp,
                            color = Color(0xFF333333),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Monitorea tu salud, y recibe consejos personalizados",
                            fontSize = 16.sp,
                            color = Color(0xFF2196F3),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.corazon),
                            contentDescription = "Logo Medicini",
                            modifier = Modifier.size(100.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Image(
                            painter = painterResource(id = R.drawable.persona),
                            contentDescription = "Imagen persona",
                            modifier = Modifier.size(100.dp)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Bienvenido a Medicini",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Controla y mejora tu salud de forma sencilla",
                        fontSize = 18.sp,
                        color = Color(0xFF333333),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Image(
                        painter = painterResource(id = R.drawable.corazon),
                        contentDescription = "Logo Medicini",
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Monitorea tu salud, y recibe consejos personalizados",
                        fontSize = 16.sp,
                        color = Color(0xFF2196F3),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Image(
                        painter = painterResource(id = R.drawable.persona),
                        contentDescription = "Imagen persona",
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Comenzar Registro",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3).copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { navController.navigate(Routes.Register) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .then(
                    if (isLandscape) Modifier.widthIn(max = 400.dp)
                    else Modifier
                ),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            Text("Comenzar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}