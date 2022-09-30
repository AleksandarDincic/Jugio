package com.thedinch.jugio;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.thedinch.jugio.databinding.FragmentCameraBinding;
import com.thedinch.jugio.databinding.FragmentResultsBinding;

import org.tensorflow.lite.task.gms.vision.detector.Detection;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ResultsFragment extends Fragment {

    MainActivity mainActivity;
    NavController navController;

    FragmentResultsBinding binding;

    ResultViewModel resultViewModel;

    CardIdentifier identifier;

    Executor identifierExecutor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mainActivity = (MainActivity) requireActivity();

        resultViewModel = new ViewModelProvider(mainActivity).get(ResultViewModel.class);

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentResultsBinding.inflate(inflater, container, false);

        ResultAdapter adapter = new ResultAdapter();

        identifierExecutor = Executors.newSingleThreadExecutor();

        resultViewModel.getResults().observe(getViewLifecycleOwner(), r -> {
            mainActivity.runOnUiThread(() -> {
                adapter.setResults(r);
            });
            for(DetectionResult rr : r){
                identifierExecutor.execute(() -> {
                    if(identifier == null){
                        identifier = new CardIdentifier(mainActivity);
                        identifier.initCardIdentifier();
                    }
                    if(rr.cardName == null){
                        identifier.identify(rr);
                        mainActivity.runOnUiThread(() -> {
                            adapter.setResults(r);
                        });
                    }
                });
            }
        });

        binding.resultsRecyclerView.setAdapter(adapter);
        binding.resultsRecyclerView.setLayoutManager(new LinearLayoutManager(mainActivity));

        return binding.getRoot();
    }

    public ResultsFragment() {
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
    }
}