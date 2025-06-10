package com.example.smartaiapp.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.smartaiapp.R

@Preview(showBackground = true)
@Composable
fun PrinceBackgroundWrapperPreview(){
    PrinceBackgroundWrapper{

    }
}

@Composable
fun PrinceBackgroundWrapper(
    modifier: Modifier = Modifier,
    child: @Composable () -> Unit,
){
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_prince_logo),
            contentDescription = null,
            modifier = Modifier
                .size(128.dp)
                .align(Alignment.Center)
                .graphicsLayer {
                    alpha = 0.5f
                }
        )
        child.invoke()
    }
}