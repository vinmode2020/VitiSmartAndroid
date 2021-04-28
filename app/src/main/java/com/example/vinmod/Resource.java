package com.example.vinmod;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;


/**
 * AppCompatActivity class that handles the Resource Page Activity.
 * It is linked to the activity_resource.xml layout file.
 */
public class Resource extends AppCompatActivity implements TabLayout.OnTabSelectedListener {

    //Declare layout elements
    private TabLayout tabLayout;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resource);



        //Initializing the tabLayout
        tabLayout = findViewById(R.id.tabLayout);


        //Create tabs with enum(1-10) titles
        tabLayout.addTab(tabLayout.newTab().setText("1"));
        tabLayout.addTab(tabLayout.newTab().setText("2"));
        tabLayout.addTab(tabLayout.newTab().setText("3"));
        tabLayout.addTab(tabLayout.newTab().setText("4"));
        tabLayout.addTab(tabLayout.newTab().setText("5"));
        tabLayout.addTab(tabLayout.newTab().setText("6"));
        tabLayout.addTab(tabLayout.newTab().setText("7"));
        tabLayout.addTab(tabLayout.newTab().setText("8"));
        tabLayout.addTab(tabLayout.newTab().setText("9"));
        tabLayout.addTab(tabLayout.newTab().setText("10"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        //Initializing viewPager
        viewPager = findViewById(R.id.pager);

        //Creating our pager adapter
        resourcePager adapter = new resourcePager(getSupportFragmentManager(), tabLayout.getTabCount());

        //Add adapter to pager
        viewPager.setAdapter(adapter);

        //Add onTabSelectedListener to swipe views
        tabLayout.setOnTabSelectedListener(this);
        viewPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener(){
                    @Override
                    public void onPageSelected(int position){
                        tabLayout.getTabAt(position).select();
                    }
                }
        );

    }

    /**
     * This method changes the ViewPager when selected.
     * @param tab retrieves which tab the user is currently on.
     */
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
    }

}



