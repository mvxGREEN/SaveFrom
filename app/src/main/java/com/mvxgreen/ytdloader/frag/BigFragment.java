package com.mvxgreen.ytdloader.frag;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.mvxgreen.ytdloader.R;


/**
 * Created by MVX on 7/6/2017.
 *
 * GOAL: Initialize dialog fragment with proper layout
 *
 * GIVEN:
 *  1) Clicked menu item id
 */

public class BigFragment extends Fragment {
    private static final String TAG = BigFragment.class.getCanonicalName();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView;
        String title = getArguments() != null ?
                getArguments().getString(getString(R.string.key_extra_menu_item_title), "") : "";

        // Check menu item title; inflate proper fragment
        if (title.equals(getString(R.string.title_menu_pp))) {
            rootView = inflater.inflate(R.layout.frag_pp, container, false);
            fillPrivacyPolicy(rootView);
        } else {
            rootView = inflater.inflate(R.layout.frag_about, container, false);
        }

        return rootView;
    }

    /**
     * Fill textview(s) with privacy policy stored in resources
     * @param rootView fragment layout
     */
    public void fillPrivacyPolicy(View rootView) {
        String[] pp = getResources().getStringArray(R.array.pp_body);
        TextView body1 = rootView.findViewById(R.id.text_pp_body);
        body1.setText(pp[0]);
    }
}
