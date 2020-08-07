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

public class PopupWebview extends DialogFragment {

    private WebView webView;
    private TextView webTitle;
    public String webURL;

    public void setWebURL(String url) {
        webURL = url;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.popup_view_web, null);

        webTitle = view.findViewById(R.id.popup_text);
        webView = view.findViewById(R.id.webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.loadUrl(webURL);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView webView, String title) {
                super.onReceivedTitle(webView, title);
                if(title != null && title.length() > 0) {
                    webTitle.setText(title);
                } else {
                    webTitle.setText("Web view");
                }
            }
        });

        builder.setView(view)
                .setNegativeButton("Close", null);

        return builder.create();
    }
}
