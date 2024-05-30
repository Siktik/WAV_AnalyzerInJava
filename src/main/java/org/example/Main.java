package org.example;

import org.jtransforms.fft.DoubleFFT_1D;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Main {

    public static double[] readWav(String filePath) throws UnsupportedAudioFileException, IOException {
        File file = new File(filePath);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
        int bytesPerFrame = audioInputStream.getFormat().getFrameSize();
        int numBytes = (int) (audioInputStream.getFrameLength() * bytesPerFrame);
        byte[] audioBytes = new byte[numBytes];
        int result = audioInputStream.read(audioBytes);
        if (result == -1) {
            throw new IOException("Could not read audio file");
        }
        audioInputStream.close();
        double[] audioData = new double[numBytes / bytesPerFrame];
        for (int i = 0; i < audioData.length; i++) {
            audioData[i] = (double) ((audioBytes[2 * i + 1] << 8) | audioBytes[2 * i] & 0xFF);
        }
        return audioData;
    }

    public static void performFFT(double[] data, int blockSize, int shiftSize, float sampleRate) {
        Runtime runtime = Runtime.getRuntime();

        // Perform garbage collection to get a more stable measurement
        runtime.gc();
        long usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();

        int numBlocks = (data.length - blockSize) / shiftSize + 1;
        double[] frequencies = new double[blockSize / 2 + 1];
        for (int i = 0; i < frequencies.length; i++) {
            frequencies[i] = i * sampleRate / blockSize;
        }

        List<Double> mainFrequencies = new ArrayList<>();
        List<Double> mainAmplitudes = new ArrayList<>();
        DoubleFFT_1D fft = new DoubleFFT_1D(blockSize);

        for (int i = 0; i < numBlocks; i++) {
            int start = i * shiftSize;
            int end = start + blockSize;
            double[] block = new double[blockSize];
            System.arraycopy(data, start, block, 0, blockSize);
            applyHanningWindow(block);
            fft.realForward(block);
            double[] spectrum = new double[blockSize / 2 + 1];
            for (int j = 0; j < blockSize / 2; j++) {
                double re = block[2 * j];
                double im = block[2 * j + 1];
                spectrum[j] = Math.sqrt(re * re + im * im) / blockSize;  // Normalize by blockSize
            }

            int mainIndex = 0;
            double maxAmplitude = 0;
            for (int j = 0; j < spectrum.length; j++) {
                if (spectrum[j] > maxAmplitude) {
                    maxAmplitude = spectrum[j];
                    mainIndex = j;
                }
            }
            mainFrequencies.add(frequencies[mainIndex]);
            mainAmplitudes.add(maxAmplitude);
        }

        runtime.gc();
        long usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = usedMemoryAfter - usedMemoryBefore;

        /*
        System.out.println("Angabe der Hauptfrequenzen und deren Amplitude:");
        for (int i = 0; i < mainFrequencies.size(); i++) {
            System.out.printf("Block %d: Hauptfrequenz = %.2f Hz, Amplitude = %.2f%n", i + 1, mainFrequencies.get(i), mainAmplitudes.get(i));
        }*/
        System.out.println("Memory used for FFT calculation: " + memoryUsed + " bytes");
    }

    private static void applyHanningWindow(double[] block) {
        int n = block.length;
        for (int i = 0; i < n; i++) {
            block[i] *= 0.5 * (1 - Math.cos(2 * Math.PI * i / (n - 1)));
        }
    }

    public static void main(String[] args) {
        String filePath = "src/main/resources/nicht_zu_laut_abspielen.wav";
        int blockSize = 2048;
        int shiftSize = blockSize / 64;

        try {
            double[] data = readWav(filePath);
            float sampleRate = 44100;  // Adjust the sample rate according to your audio file
            performFFT(data, blockSize, shiftSize, sampleRate);
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
    }
}
