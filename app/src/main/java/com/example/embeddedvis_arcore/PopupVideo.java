package com.example.embeddedvis_arcore;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.TextView;

public class PopupVideo extends DialogFragment {

    private WebView videoView;
    private TextView videoTitle;
    public String videoID, videoURL;

    public void setVideoID(String id) {
        videoID = id;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.popup_view_video, null);

        videoTitle = view.findViewById(R.id.popup_text);
        videoView = view.findViewById(R.id.youTubeView);

        videoURL = "https://www.youtube.com/embed/" + videoID;

        videoView.getSettings().setJavaScriptEnabled(true);
        videoView.loadUrl(videoURL);
        videoView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView webView, String title) {
                super.onReceivedTitle(webView, title);
                if(title != null && title.length() > 0) {
                    videoTitle.setText(title);
                } else {
                    videoTitle.setText(R.string.video);
                }
            }
        });

        builder.setView(view)
                .setNegativeButton("Close", null);

        return builder.create();
    }

}
