package com.aldo.itemmanaginapp.StartActivity;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by aldo on 2017-08-04.
 */

public class StartFragment extends Fragment implements StartContract.View {
    @Override
    public void setPresenter(StartContract.Presenter presenter) {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
