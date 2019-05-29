package com.codebosses.flicks.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.codebosses.flicks.R;
import com.codebosses.flicks.endpoints.EndpointKeys;
import com.codebosses.flicks.endpoints.EndpointUrl;
import com.codebosses.flicks.services.BackgroundNotificationService;
import com.commit451.youtubeextractor.YouTubeExtraction;
import com.commit451.youtubeextractor.YouTubeExtractor;
import com.downloader.Error;
import com.downloader.OnCancelListener;
import com.downloader.OnDownloadListener;
import com.downloader.OnPauseListener;
import com.downloader.OnProgressListener;
import com.downloader.OnStartOrResumeListener;
import com.downloader.PRDownloader;
import com.downloader.Progress;
import com.pierfrancescosoffritti.androidyoutubeplayer.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.player.YouTubePlayerView;
import com.pierfrancescosoffritti.androidyoutubeplayer.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.player.listeners.YouTubePlayerInitListener;

import java.io.File;
import java.util.UUID;

public class TrailerActivity extends AppCompatActivity {

    public static final String PROGRESS_UPDATE = "progress_update";

    //    Android fields....
    @BindView(R.id.youtubePlayerViewTrailer)
    YouTubePlayerView youTubePlayerView;
    private YouTubePlayer youTubePlayer;
    @BindView(R.id.imageViewDownloadTrailer)
    AppCompatImageView imageViewDownload;

    //    Instance fields....
    private String youtubeKey;
    private YouTubeExtractor youTubeExtractor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trailer);
        ButterKnife.bind(this);

        if (getIntent() != null) {
            youtubeKey = getIntent().getStringExtra(EndpointKeys.YOUTUBE_KEY);

            youTubePlayerView.initialize(new YouTubePlayerInitListener() {
                @Override
                public void onInitSuccess(@NonNull YouTubePlayer youTubePlayer) {
                    youTubePlayer.addListener(new AbstractYouTubePlayerListener() {
                        @Override
                        public void onReady() {
                            super.onReady();
                            TrailerActivity.this.youTubePlayer = youTubePlayer;
                            youTubePlayer.loadVideo(youtubeKey, 0);
                        }

                    });
                }
            }, true);
            youTubeExtractor = new YouTubeExtractor.Builder().build();
            registerReceiver();
        }
    }

    private void registerReceiver() {

        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PROGRESS_UPDATE);
        bManager.registerReceiver(mBroadcastReceiver, intentFilter);

    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(PROGRESS_UPDATE)) {

                boolean downloadComplete = intent.getBooleanExtra("downloadComplete", false);
                //Log.d("API123", download.getProgress() + " current progress");

                if (downloadComplete) {

                    Toast.makeText(getApplicationContext(), "File download completed", Toast.LENGTH_SHORT).show();


                }
            }
        }
    };


    private void bindVideoResult(YouTubeExtraction result, Throwable throwable) {
        String videoUrl = result.getVideoStreams().get(0).getUrl();
        downloadImage(videoUrl);
//        Log.d("OnSuccess", "Got a result with the best url: $videoUrl")
//        Glide.with(this)
//                .load(result.thumbnails.first().url)
//                .into(imageView)
//        videoView.setVideoURI(Uri.parse(videoUrl))
    }

    @OnClick(R.id.imageViewCloseTrailer)
    public void onCloseClick(View view) {
        finish();
    }

    @SuppressLint("CheckResult")
    @OnClick(R.id.imageViewDownloadTrailer)
    public void onDownloadClick(View view) {
        Toast.makeText(this, "Downloading...", Toast.LENGTH_SHORT).show();
        youTubeExtractor.extract(youtubeKey)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((youTubeExtraction, throwable) -> bindVideoResult(youTubeExtraction, throwable));
    }

    private void downloadImage(String path) {
        Intent intent = new Intent(this, BackgroundNotificationService.class);
        intent.putExtra("path", path);
        startService(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (youTubePlayer != null) {
            youTubePlayer.pause();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (youTubePlayer != null) {
            youTubePlayer.play();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        youTubePlayerView.release();
    }
}
