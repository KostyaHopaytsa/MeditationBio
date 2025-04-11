package com.example.meditationbio;

import android.graphics.ImageFormat;
import android.media.Image;
import android.util.Log;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

public class PPGAnalyzer implements ImageAnalysis.Analyzer {

    @androidx.camera.core.ExperimentalGetImage
    @Override
    public void analyze(ImageProxy image) {
        Image img = image.getImage();
        if (img != null && img.getFormat() == ImageFormat.YUV_420_888) {
            ByteBuffer buffer = img.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            long sum = 0;
            for (byte b : data) {
                sum += (b & 0xFF);
            }

            double average = sum / (double) data.length;
            Log.d("PPG", "Середня яскравість: " + average);
        }

        image.close();
    }
}