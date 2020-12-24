package com.example.vinmod;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;


/**
 * This method allows uses to swap between the different resourceTab constructors
 *
 * @author David Simmons
 */
public class resourcePager extends FragmentStatePagerAdapter {

    int tabCount;

    /**
     * Constructor for resourcePager class.
     *
     * @param fm which fragment is loaded
     * @param tabCount counts which tab you are currently on
     */
    public resourcePager(FragmentManager fm, int tabCount){
        super(fm);

        //initializing tab count
        this.tabCount = tabCount;
    }

    /**
     * This method will call the constructor of the different resourceTab classes
     *
     * @param pos which resourceTab class constructor is called
     * @return resourceTab class constructor
     */
    public Fragment getItem(int pos){
        //Returning the current tabs
        switch(pos){
            case 0:
                resourceTab1 tab1 = new resourceTab1();
                return tab1;
            case 1:
                resourceTab2 tab2 = new resourceTab2();
                return tab2;
            case 2:
                resourceTab3 tab3 = new resourceTab3();
                return tab3;
            default:
                return null;
        }
    }

    /**
     * This method returns the tabCount
     *
     * @return which tab you are currently on
     */
    @Override
    public int getCount() {
        return tabCount;
    }
}
