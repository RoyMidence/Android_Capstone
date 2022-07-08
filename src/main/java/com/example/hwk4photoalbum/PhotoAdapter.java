package com.example.hwk4photoalbum;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.MyViewHolder> {
    private List<Photo> mPhotoList;
    private itemClickInterface mItemClickInterface;

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ImageView imageViewPhoto;
        public TextView textViewName;
        public itemClickInterface itemClickInterface;
        CardView mainLayout;

        public MyViewHolder(View view,itemClickInterface itemClickInterface) {
            super(view);
            imageViewPhoto = (ImageView) view.findViewById(R.id.imageViewPhoto);
            textViewName = (TextView) view.findViewById(R.id.textViewName);
            mainLayout = itemView.findViewById(R.id.mainLayout);
            this.itemClickInterface = itemClickInterface;

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {itemClickInterface.onItemClick(getBindingAdapterPosition());}
    }

    public PhotoAdapter(List<Photo> photos, itemClickInterface itemClickInterface) {
        mPhotoList = photos;
        mItemClickInterface = itemClickInterface;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.photo_recyclerview_item,parent,false);
        return new MyViewHolder(itemView,mItemClickInterface);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Bitmap bitmap = mPhotoList.get(position).getPicture();
        String name = mPhotoList.get(position).getName();

        holder.imageViewPhoto.setImageBitmap(bitmap);
        holder.textViewName.setText(name);
    }

    @Override
    public int getItemCount() {return mPhotoList.size();}

    public void setData(List<Photo> photos) {
        mPhotoList = photos;
        notifyDataSetChanged();
    }

    public interface itemClickInterface {
        void onItemClick(int position);
    }
}
