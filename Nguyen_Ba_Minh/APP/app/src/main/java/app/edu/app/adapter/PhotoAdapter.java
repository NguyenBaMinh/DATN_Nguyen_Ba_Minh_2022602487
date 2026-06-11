package app.edu.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import app.edu.app.R;
import app.edu.app.model.Photo;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>{
        ArrayList<Photo> list;

        public PhotoAdapter(ArrayList<Photo> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_iange_show, parent, false);
            return new PhotoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
            Photo photo = list.get(position);
            if (photo == null) {
                return;
            }
            holder.img.setImageResource(photo.getSrcImage());
        }

        @Override
        public int getItemCount() {
            if (list != null) {
                return list.size();
            }
            return 0;
        }

        public static class PhotoViewHolder extends RecyclerView.ViewHolder {
            ImageView img;

            public PhotoViewHolder(@NonNull View itemView) {
                super(itemView);
                img = itemView.findViewById(R.id.imageShow);
            }
        }
}
