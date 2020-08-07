package com.example.embeddedvis_arcore;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;

public class ARFragment extends ArFragment {
    private static final String TAG = "ARFragment";

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        return view;
    }

    @Override
    protected Config getSessionConfiguration(Session session) {

        Config config = session.getConfig();
        config.setFocusMode(Config.FocusMode.AUTO);
        session.configure(config);

        return config;
    }

}