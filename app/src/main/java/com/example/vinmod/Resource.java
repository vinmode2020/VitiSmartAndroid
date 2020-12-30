package com.example.vinmod;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;


/**
 * This method creates the functionality behind the Resource Button on the VINMOD homepage
 *
 * @author David Simmons, Mohamed Ibrahim
 */
public class Resource extends AppCompatActivity implements TabLayout.OnTabSelectedListener {

    private TabLayout tabLayout;
    private ViewPager viewPager;


    /**
     * This method sets all the functionality behind the activity_resource.xml file.
     *
     * @param savedInstanceState Instance of the activity_resource.xml when created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resource);



        //Initializing the tabLayout
        tabLayout = findViewById(R.id.tabLayout);

        //Adding tabs using addTab()
        tabLayout.addTab(tabLayout.newTab().setText("Tab 1"));
        tabLayout.addTab(tabLayout.newTab().setText("Tab 2"));
        tabLayout.addTab(tabLayout.newTab().setText("Tab 3"));
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



