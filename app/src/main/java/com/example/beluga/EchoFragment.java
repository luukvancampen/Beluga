package com.example.beluga;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.health.SystemHealthManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.beluga.databinding.EchoFragmentBinding;
import com.example.beluga.databinding.FragmentFirstBinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Objects;

public class EchoFragment extends Fragment {
    private EchoFragmentBinding binding;
    private boolean beeping = false;
    private AudioRecord record;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = EchoFragmentBinding.inflate(inflater, container, false);
        binding.echoStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {


                    customBeep(400, 0.2);
                    customBeep(500, 0.2);
                    customBeep(600, 0.2);
                    customBeep(800, 0.2);
                    customBeep(1000, 0.2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        binding.beepRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    checkRecordingPermission();
                    customBeepRecord(600, 0.2, 20, 1000, "record.pcm");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        binding.customBeepRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
                LayoutInflater inflater = requireActivity().getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.custom_beep_record_layout, null);

                EditText beepDurationEditText = dialogView.findViewById(R.id.beepDurationEditText);
                EditText beepFrequencyEditText = dialogView.findViewById(R.id.beepFrequencyEditText);
                EditText recordDurationEditText = dialogView.findViewById(R.id.recordDurationEditText);
                EditText recordOffsetEditText = dialogView.findViewById(R.id.recordOffsetEditText);
                EditText recordNameEditText = dialogView.findViewById(R.id.recordNameEditText);

                Button doBeepButton = dialogView.findViewById(R.id.customBeepButton);
                doBeepButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
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
                    }
                });

                dialogBuilder.setView(dialogView);
                AlertDialog dialog = dialogBuilder.create();
                dialog.show();
            }
        });

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build())
                .setBufferSizeInBytes(1000000)
                .build();
        double samples = sampleRate * (duration / 1000);
        double[] sample = new double[(int) samples];

        byte[] generatedSound = new byte[(int) (2*samples)];

        for (int i = 0; i < samples; i++) {
            sample[i] = Math.sin(2 * Math.PI * i / Math.ceil(sampleRate / frequency));
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
        Thread writeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioToFile(recordName);
            }
        });

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
        record.startRecording();
        beeping = true;
        writeThread.start();
        Thread.sleep(duration);
        record.stop();
        beeping = false;
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

    void writeAudioToFile(String recordName) {

        String path = "belugaRecording.pcm";

        FileOutputStream os = null;
        try {
            System.out.println(Environment.getExternalStorageDirectory().getAbsolutePath());
//            os = requireContext().openFileOutput("recording.pcm", Context.MODE_PRIVATE);
            os = new FileOutputStream(new File(Environment.getExternalStorageDirectory().getAbsolutePath(), recordName));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        short[] shortData = new short[2 * 800];

        while (beeping) {
            // gets the voice output from microphone to byte format

            record.read(shortData, 0, 1600);
//            System.out.println("Short wirting to file" + Arrays.toString(shortData));
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = short2byte(shortData);
                os.write(bData, 0, 1600 * 2);
                System.out.println("WROTE STUFF");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
