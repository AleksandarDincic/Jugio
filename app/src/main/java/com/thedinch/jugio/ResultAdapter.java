package com.thedinch.jugio;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.thedinch.jugio.databinding.ViewHolderResultBinding;

import java.util.ArrayList;
import java.util.List;

public class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ResultViewHolder>{

    List<DetectionResult> results = new ArrayList<>();

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ViewHolderResultBinding binding = ViewHolderResultBinding.inflate(layoutInflater, parent, false);
        return new ResultViewHolder(binding);
    }

    public void setResults(List<DetectionResult> results){
        this.results = results;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        holder.bind(results.get(position));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public class ResultViewHolder extends RecyclerView.ViewHolder{

        public ViewHolderResultBinding binding;

        public ResultViewHolder(@NonNull ViewHolderResultBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(DetectionResult result){
            binding.capturedImage.setImageBitmap(result.bitmap);
            if(result.cardName != null){
                binding.resultCardName.setText(result.cardName);
            }
        }
    }
}
