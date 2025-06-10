package com.example.smartaiapp.utils

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.smartaiapp.R

@Preview(showBackground = true)
@Composable
fun CommonButtonField(){
    CommonButton(
        "Test",
        onClick = {}
    )
}

@Composable
fun CommonButton(
    label:String,
    onClick:()->Unit,
    backgroundColor:Color = colorResource(R.color.color_2d3494),
    modifier: Modifier = Modifier
){
    Button(
        onClick = {
            onClick.invoke()
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        modifier = modifier
    ) {
        Text(label)
    }
}