package com.example.vinmod;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;


/**
 *  The method for creating tab2.xml in activity_resource.xml
 *  This class extends Fragment
 *
 * @author David Simmons
 */
public class resourceTab2 extends Fragment {

    /**
     * This method will create the View of tab2.xml
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return returns the formatted tab2.xml
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tab2, container, false);
    }
}