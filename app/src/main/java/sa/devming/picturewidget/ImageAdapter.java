package sa.devming.picturewidget;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ImageAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private Context context;
    private ArrayList<Uri> imageList;
    private boolean[] isCheck;

    public ImageAdapter(Context context) {
        this.context = context;
        this.inflater = (LayoutInflater.from(context));
    }

    public void setImageList(ArrayList<Uri> imageList) {
        this.imageList = imageList;
        this.isCheck = new boolean[imageList.size()];
    }

    public boolean[] getIsCheck() {
        return isCheck;
    }

    @Override
    public int getCount() {
        return (imageList == null) ? 0 : imageList.size();
    }

    @Override
    public Object getItem(int position) {
        return (imageList != null && imageList.size() > position) ? imageList.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.griditem, null);
            holder.itemImage = (ImageView)convertView.findViewById(R.id.itemImage);
            holder.itemCheck = (CheckBox)convertView.findViewById(R.id.itemCheck);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.itemImage.setId(position);
        holder.itemCheck.setId(position);
        holder.itemImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageView iv = (ImageView)v;
                int position = iv.getId();
                if (isCheck[position]) {
                    holder.itemCheck.setChecked(false);
                    isCheck[position] = false;
                } else {
                    holder.itemCheck.setChecked(true);
                    isCheck[position] = true;
                }
            }
        });
        holder.itemCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox cb = (CheckBox)v;
                int position = cb.getId();
                if (isCheck[position]) {
                    cb.setChecked(false);
                    isCheck[position] = false;
                } else {
                    cb.setChecked(true);
                    isCheck[position] = true;
                }
            }
        });
        Uri uri = Uri.parse(imageList.get(position).toString());
        Picasso
                .with(context)
                .load(uri)
                .resizeDimen(R.dimen.grid_x, R.dimen.grid_y)
                .centerCrop()
                .placeholder(R.drawable.no_image)
                .into(holder.itemImage);
        holder.itemCheck.setChecked(isCheck[position]);
        holder.id = position;
        return convertView;
    }

    class ViewHolder {
        ImageView itemImage;
        CheckBox itemCheck;
        int id;
    }
}
