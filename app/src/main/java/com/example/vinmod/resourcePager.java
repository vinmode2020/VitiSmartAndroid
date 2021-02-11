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
            case 3:
                resourceTab4 tab4 = new resourceTab4();
                return tab4;
            case 4:
                resourceTab5 tab5 = new resourceTab5();
                return tab5;
            case 5:
                resourceTab6 tab6 = new resourceTab6();
                return tab6;
            case 6:
                resourceTab7 tab7 = new resourceTab7();
                return tab7;
            case 7:
                resourceTab8 tab8 = new resourceTab8();
                return tab8;
            case 8:
                resourceTab9 tab9 = new resourceTab9();
                return tab9;
            case 9:
                resourceTab10 tab10 = new resourceTab10();
                return tab10;
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
