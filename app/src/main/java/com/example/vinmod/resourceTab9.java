package com.example.vinmod;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;


/**
 *  The method for creating tab1.xml in activity_resource.xml
 *  This class extends Fragment
 *
 * @author David Simmons
 */
public class resourceTab9 extends Fragment {

    /**
     * This method will create the View of tab1.xml
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return returns the formatted tab1.xml
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tab9, container, false);
    }
}