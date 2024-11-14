package com.bibek.bytestream.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SampleButtonLayout(
    sample1OnClick: () -> Unit,
    sample2OnClick: () -> Unit,
    sample3OnClick: () -> Unit,
    sample4OnClick: () -> Unit,
    sample5OnClick: () -> Unit,
    sample6OnClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
    ) {
        // First Row of Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp)
        ) {
            Button(
                onClick = sample1OnClick,
                modifier = Modifier
                    .weight(1f)
                    .padding(5.dp)
            ) {
                Text("Sample 1")
            }

            Button(
                onClick =sample2OnClick,
                modifier = Modifier
                    .weight(1f)
                    .padding(5.dp)
            ) {
                Text("Sample 2")
            }

            Button(
                onClick = sample3OnClick,
                modifier = Modifier
                    .weight(1f)
                    .padding(5.dp)
            ) {
                Text("Sample 3")
            }
        }

        // Second Row of Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp)
        ) {
            Button(
                onClick = sample4OnClick,
                modifier = Modifier
                    .weight(1f)
                    .padding(5.dp)
            ) {
                Text("Sample 4")
            }

            Button(
                onClick = sample5OnClick,
                modifier = Modifier
                    .weight(1f)
                    .padding(5.dp)
            ) {
                Text("Sample 5")
            }

            Button(
                onClick = sample6OnClick,
                modifier = Modifier
                    .weight(1f)
                    .padding(5.dp)
            ) {
                Text("Sample 6")
            }
        }
    }
}
