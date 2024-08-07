package com.kouts.spiri.smartalert.Functionality.Fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kouts.spiri.smartalert.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends Fragment {

    View settingsView;
    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance(String param1, String param2) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        settingsView = inflater.inflate(R.layout.fragment_settings, container, false);

        LinearLayout personalInfo = settingsView.findViewById(R.id.personal_info);
        personalInfo.setOnClickListener(v -> menuClick("personal_info"));

        LinearLayout myAddresses = settingsView.findViewById(R.id.my_locations);
        myAddresses.setOnClickListener(v -> menuClick("my_addresses"));

        LinearLayout questionnaire = settingsView.findViewById(R.id.questionnaire);
        questionnaire.setOnClickListener(v -> menuClick("questionnaire"));


        return settingsView;
    }

    public void menuClick(String option) {
        switch (option) {
            case "personal_info":
                //here we open the edit info of the user
                PersonalInfoFragment personalInfoFragment = new PersonalInfoFragment();
                personalInfoFragment.show(getChildFragmentManager(), "personalInfo");
                break;
            case "my_addresses":
                //here we open the edit addresses of the user
                MyAddressesFragment myAddressesFragment = new MyAddressesFragment();
                myAddressesFragment.show(getChildFragmentManager(), "myAddresses");
                break;
            case "questionnaire":
                QuestionnaireFragment questionnaireFragment = new QuestionnaireFragment();
                questionnaireFragment.show(getChildFragmentManager(), "questionnaire");
                break;
        }
    }
}