package com.thedinch.jugio;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ResultViewModel extends ViewModel {

    private MutableLiveData<List<DetectionResult>> results = new MutableLiveData<>(new ArrayList<>());


    public LiveData<List<DetectionResult>> getResults(){
        return results;
    }

    public void setResults(List<DetectionResult> newRes){
        results.setValue(newRes);
    }

}
