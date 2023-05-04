package com.example.beluga;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.beluga.databinding.EchoFragmentBinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class EchoFragment extends Fragment {
    private boolean beeping = false;
    private AudioRecord record;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        com.example.beluga.databinding.EchoFragmentBinding binding = EchoFragmentBinding.inflate(inflater, container, false);
        binding.customBeepRecordButton.setOnClickListener(view -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
            LayoutInflater inflater1 = requireActivity().getLayoutInflater();
            View dialogView = inflater1.inflate(R.layout.custom_beep_record_layout, null);

            EditText beepDurationEditText = dialogView.findViewById(R.id.beepDurationEditText);
            EditText beepFrequencyEditText = dialogView.findViewById(R.id.beepFrequencyEditText);
            EditText recordDurationEditText = dialogView.findViewById(R.id.recordDurationEditText);
            EditText recordOffsetEditText = dialogView.findViewById(R.id.recordOffsetEditText);
            EditText recordNameEditText = dialogView.findViewById(R.id.recordNameEditText);

            Button doBeepButton = dialogView.findViewById(R.id.customBeepButton);
            doBeepButton.setOnClickListener(view1 -> {
                try {
                    customBeepRecord(
                            Integer.parseInt(beepFrequencyEditText.getText().toString()),
                            Integer.parseInt(beepDurationEditText.getText().toString()),
                            Integer.parseInt(recordOffsetEditText.getText().toString()),
                            Integer.parseInt(recordDurationEditText.getText().toString()),
                            recordNameEditText.getText().toString());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

            dialogBuilder.setView(dialogView);
            AlertDialog dialog = dialogBuilder.create();
            dialog.show();
        });

        binding.customChirpButton.setOnClickListener(view -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
            LayoutInflater inflater12 = requireActivity().getLayoutInflater();
            View dialogView = inflater12.inflate(R.layout.custom_chirp_record_layout, null);

            EditText beepDurationEditText = dialogView.findViewById(R.id.beepDurationEditText);
            EditText chirpFrequencyStartEditText = dialogView.findViewById(R.id.chirpFrequencyStartEditText);
            EditText chirpFrequencyEndEditText = dialogView.findViewById(R.id.chirpFrequencyEndEditText);
            EditText recordDurationEditText = dialogView.findViewById(R.id.recordDurationEditText);
            EditText recordOffsetEditText = dialogView.findViewById(R.id.recordOffsetEditText);
            EditText recordNameEditText = dialogView.findViewById(R.id.recordNameEditText);

            Button doBeepButton = dialogView.findViewById(R.id.customBeepButton);
            doBeepButton.setOnClickListener(view12 -> {
                try {
                    customChirpRecord(
                            Integer.parseInt(chirpFrequencyStartEditText.getText().toString()),
                            Integer.parseInt(chirpFrequencyEndEditText.getText().toString()),
                            Integer.parseInt(beepDurationEditText.getText().toString()),
                            Integer.parseInt(recordOffsetEditText.getText().toString()),
                            Integer.parseInt(recordDurationEditText.getText().toString()),
                            recordNameEditText.getText().toString());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

            dialogBuilder.setView(dialogView);
            AlertDialog dialog = dialogBuilder.create();
            dialog.show();

        });

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    void gatherTrainingDataChirp(String label, double frequencyStart, double frequencyEnd, long chirpDuration, long timeBetweenChirps) {

    }

    void customBeep(double frequency, double duration) throws InterruptedException {
        int sampleRate = 44100;

        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(10 * AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT))
                .build();
        double samples = sampleRate * (duration / 1000);
        System.out.println("Samples: " + samples);
        double[] sample = new double[(int) samples];

        byte[] generatedSound = new byte[(int) (2*samples)];

        for (int i = 0; i < samples - 1; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / frequency));
        }

        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSound[idx++] = (byte) (val & 0x00ff);
            generatedSound[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
        System.out.println("Generated sound lenght: " + generatedSound.length);

        track.write(generatedSound, 0, generatedSound.length);
        track.play();
        Thread.sleep((long) duration);
        System.out.println("DONE PLAYING");
    }

    void customChirp(int frequenceyStart, int frequencyEnd, double duration) throws InterruptedException {
        int sampleRate = 44100;
        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(1000000)
                .build();

        double samples = Math.ceil(sampleRate * (duration / 1000));
        System.out.println("Samples: " + samples);
        double[] sample = new double[(int) samples];

        byte[] generatedSound = new byte[(int) (2*samples)];

        int frequencies = frequencyEnd - frequenceyStart;
        double samplesPerFrequency = samples / frequencies;
        for (int i = 0; i < samples; i++) {

            sample[i] = Math.sin(2 * Math.PI * i / Math.ceil(44100.0 / (frequenceyStart + Math.floorDiv(i, (int) Math.floor(samplesPerFrequency) + 1))));
        }
        int idx = 0;
        for (final double dVal : sample) {
            final short val = (short) ((dVal * 32767));
            generatedSound[idx] = (byte) (val & 0x00ff);
            idx++;
            generatedSound[idx] = (byte) ((val & 0xff00) >>> 8);
            idx++;
        }


        track.write(generatedSound, 0, generatedSound.length);
        track.play();
        Thread.sleep((long) duration);
        System.out.println("DONE PLAYING");

    }

    void customBeepRecord(int frequency, double beepDuration, long recordingOffset, long recordingDuration, String recordName) throws InterruptedException {
        Thread writeThread = new Thread(() -> writeAudioToFile(recordName, (long) Math.ceil((recordingDuration / 1000.0) * 44100)));

        try {
            record = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    10 * AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            );
        } catch (SecurityException e) {
            System.out.println("permission not granted");
        }

        customBeep(frequency, beepDuration);
        customRecord(recordingDuration, recordingOffset, writeThread);

    }

    void customChirpRecord(int frequencyStart, int frequencyEnd, double beepDuration, long recordingOffset, long recordingDuration, String recordName) throws InterruptedException {
        Thread writeThread = new Thread(() -> writeAudioToFile(recordName, (long) Math.ceil((recordingDuration / 1000.0) * 44100)));

        try {
            record = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    10 * AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            );
        } catch (SecurityException e) {
            System.out.println("permission not granted");
        }

        customChirp(frequencyStart, frequencyEnd, beepDuration);
        customRecord(recordingDuration, recordingOffset, writeThread);

    }


    void checkRecordingPermission() {
        String recordPermission = Manifest.permission.RECORD_AUDIO;
        int checkVal = requireContext().checkCallingOrSelfPermission(recordPermission);
        int checkWritePermission = requireContext().checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (checkVal==PackageManager.PERMISSION_DENIED) {
            Toast.makeText(getContext(), "Recording permission not granted", Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, 100);

        } if (checkWritePermission == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }
        else {
            Toast.makeText(getContext(), "Recording permission granted!", Toast.LENGTH_LONG).show();
        }
    }


    void customRecord(long duration, long offset, Thread writeThread) throws InterruptedException {
        Thread.sleep(offset);
        long recordStartTime = System.nanoTime();

        writeThread.start();
        Thread.sleep(duration);
        beeping = false;
        System.out.println(" ================================= beeping = false ==========================================");
        System.out.println("Recording time: " + ((System.nanoTime() - recordStartTime) / 1000000));

        record.stop();
        System.out.println("DONE RECORDING");
    }

    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    void writeAudioToFile(String recordName, long samples) {


        FileOutputStream os = null;
        try {
            System.out.println(Environment.getExternalStorageDirectory().getAbsolutePath());
            os = new FileOutputStream(new File(Environment.getExternalStorageDirectory().getAbsolutePath(), recordName), false);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        short[] shortData = new short[(int) samples];

        long writeStartTime = System.nanoTime();
        record.startRecording();
        beeping = true;
        while (beeping) {
            record.read(shortData, 0, AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) / 2);
            try {
                byte[] bData = short2byte(shortData);
                assert os != null;
                os.write(bData, 0, AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT));
                System.out.println("Still trying to write stuff");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
            System.out.println("Done writing: " + ((System.nanoTime() - writeStartTime) / 1000000));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
